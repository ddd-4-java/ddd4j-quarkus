package io.ddd4j.quarkus.mq.core;

import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.annotation.MQEventListener;
import io.ddd4j.mq.event.MQEventSerialization;
import io.ddd4j.mq.event.MQEventStorer;
import io.ddd4j.mq.listener.MQListener;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Quarkus MQ 监听器注册器（对齐 ddd4j-mq-spring 的 {@code MQListenerBeanPostProcessor}）。
 *
 * <p>在应用启动时（{@link StartupEvent}），扫描所有 CDI Bean 中标注了
 * {@link MQEventListener} 的方法，构建 {@link MQListener} 定义，然后委托给
 * 活跃的 {@link MQClient#init(List, MQProperties, MQEventSerialization, MQEventStorer)}
 * 完成生产者初始化和消费者注册。
 *
 * <h3>核心流程（与 ddd4j-mq-spring 对齐）</h3>
 * <pre>
 *   @Observes StartupEvent
 *       ↓
 *   扫描 @MQEventListener → List&lt;MQListener&gt;
 *       ↓
 *   查找活跃 MQClient（由各 broker CdiProducer 暴露）
 *       ↓
 *   MQClient.init(listeners, properties, serialization, storer)
 *       ↓
 *   生产者注册到 BaseContext + 消费者启动
 * </pre>
 *
 * <p>关键设计决策：不重写 consume 管道，完全复用 {@link MQClient#consume(MQListener, io.ddd4j.mq.event.MQEvent, io.ddd4j.mq.message.Acknowledgment)}
 * 的内置逻辑（策略匹配 → 租户注入 → 持久化 → 反射调用 → 异常解包）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@ApplicationScoped
public class QuarkusMQListenerRegistrar {

    private static final Logger log = Logger.getLogger(QuarkusMQListenerRegistrar.class);

    @Inject
    BeanManager beanManager;

    @Inject
    MQProperties mqProperties;

    @Inject
    MQEventSerialization serialization;

    /**
     * 可选注入 MQEventStorer（业务项目可提供实现，缺失时 MQClient 内部跳过持久化）。
     */
    @Inject
    Instance<MQEventStorer> storerInstance;

    void onStart(@Observes StartupEvent event) {
        if (!mqProperties.isEnabled()) {
            log.info("MQ disabled (ddd4j.mq.enabled=false), skipping listener registration");
            return;
        }

        // 1. 扫描所有 @MQEventListener 方法，构建 MQListener 定义
        List<MQListener> listeners = scanListeners();
        if (listeners.isEmpty()) {
            log.info("No @MQEventListener methods found, skipping MQ initialization");
            return;
        }

        // 2. 查找活跃的 MQClient（由各 broker CdiProducer 暴露）
        Optional<MQClient> clientOpt = findActiveClient();
        if (clientOpt.isEmpty()) {
            log.warnf("No MQClient found for broker '%s', skipping initialization. " +
                    "Did you add the corresponding ddd4j-quarkus-mq-<broker> dependency?",
                    mqProperties.getBroker());
            return;
        }

        MQClient client = clientOpt.get();

        // 3. 获取可选的 MQEventStorer
        MQEventStorer storer = storerInstance.isResolvable() ? storerInstance.get() : null;

        // 4. 委托给 MQClient.init() 完成生产者初始化 + 消费者注册
        // 这一行等价于 ddd4j-mq-spring 的 BeanPostProcessor + MQListenerRegistrar 全部工作
        try {
            client.init(listeners, mqProperties, serialization, storer);
            log.infof("MQ initialized: broker=%s, listeners=%d", client.impl(), listeners.size());
        } catch (Exception e) {
            log.errorf(e, "Failed to initialize MQClient for broker '%s'", client.impl());
        }
    }

    /**
     * 扫描所有 CDI Bean 中标注了 {@link MQEventListener} 的方法。
     *
     * <p>对齐 ddd4j-mq-spring 的 {@code MQListenerBeanPostProcessor} 扫描逻辑：
     * 遍历所有 Object 类型的 Bean，检查其类和父类的声明方法。
     */
    private List<MQListener> scanListeners() {
        List<MQListener> listeners = new ArrayList<>();
        for (Bean<?> bean : beanManager.getBeans(Object.class)) {
            // 跳过框架内部 Bean（Ambiguous / Decorator / Interceptor 等）
            if (bean.getBeanClass() == null) {
                continue;
            }
            scanClass(beanManager.getReference(bean, bean.getBeanClass(),
                    beanManager.createCreationalContext(null)), bean.getBeanClass(), listeners);
        }
        return listeners;
    }

    /**
     * 递归扫描类及其父类中的 @MQEventListener 方法。
     */
    private void scanClass(Object bean, Class<?> clazz, List<MQListener> listeners) {
        if (clazz == null || clazz == Object.class) {
            return;
        }
        for (Method method : clazz.getDeclaredMethods()) {
            MQEventListener ann = method.getAnnotation(MQEventListener.class);
            if (ann != null) {
                MQListener listener = MQListener.of(bean, method, ann);
                listeners.add(listener);
                log.debugf("Found @MQEventListener: %s.%s (topic=%s, tags=%s)",
                        clazz.getSimpleName(), method.getName(), ann.topic(), ann.tags());
            }
        }
        // 扫描接口默认方法（接口默认实现中的 @MQEventListener）
        for (Class<?> iface : clazz.getInterfaces()) {
            scanClass(bean, iface, listeners);
        }
        // 扫描父类
        scanClass(bean, clazz.getSuperclass(), listeners);
    }

    /**
     * 查找与当前配置 broker 匹配的活跃 MQClient。
     *
     * <p>优先匹配 {@link MQClient#impl()} 与 {@link MQProperties#getBroker()} 一致的实例。
     * 如果没有精确匹配，尝试用 {@link io.ddd4j.mq.BrokerType#from(String)} 解析后再匹配。
     */
    private Optional<MQClient> findActiveClient() {
        String configuredBroker = mqProperties.getBroker();
        Instance<MQClient> clients = beanManager.createInstance().select(MQClient.class);
        if (clients.isUnsatisfied()) {
            return Optional.empty();
        }
        // 精确匹配 impl()
        for (MQClient client : clients) {
            if (configuredBroker.equalsIgnoreCase(client.impl())) {
                return Optional.of(client);
            }
        }
        // 用 BrokerType 做模糊匹配
        try {
            io.ddd4j.mq.BrokerType brokerType = io.ddd4j.mq.BrokerType.from(configuredBroker);
            for (MQClient client : clients) {
                if (brokerType.name().equalsIgnoreCase(client.impl())) {
                    return Optional.of(client);
                }
            }
        } catch (Exception ignored) {
            // BrokerType.from() 可能抛异常，忽略
        }
        // 如果只有 1 个 MQClient，直接返回
        List<MQClient> all = new ArrayList<>();
        clients.forEach(all::add);
        if (all.size() == 1) {
            log.infof("Single MQClient found (impl=%s), using it regardless of configured broker '%s'",
                    all.get(0).impl(), configuredBroker);
            return Optional.of(all.get(0));
        }
        return Optional.empty();
    }
}