package io.ddd4j.quarkus.mq.core;

import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.annotation.MQEventListener;
import io.ddd4j.mq.event.MQEventSerialization;
import io.ddd4j.mq.event.MQEventStorer;
import io.ddd4j.mq.listener.MQListener;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.annotations.RegisterForReflection;
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
 * <h3>GraalVM native-image 注意事项</h3>
 * <p>本类运行时经 {@code getDeclaredMethods()} 扫描业务 Bean 中标注
 * {@link MQEventListener} 的方法，{@code MQClient#consume} 再经 {@code Method#invoke}
 * 反射调用。被扫描的监听器类由业务方提供，native 反射注册责任在业务方：构建
 * native image 前在 {@code application.properties} 加（包名替换为业务监听器所在包）：
 * <pre>{@code
 * quarkus.native.reflection.include-patterns=com.example.listener.*
 * }</pre>
 * 未注册时 JVM 模式正常，native 模式下监听器方法扫描不到（静默跳过）或消费路径抛
 * {@code NoSuchMethodException}。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@ApplicationScoped
@RegisterForReflection(methods = true)
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
     * 遍历所有 Object 类型的 Bean，检查其类和父类的声明方法。</p>
     *
     * <p><b>注意</b>：仅当某个 Bean 的类（含父类/接口）声明了 {@link MQEventListener} 方法时，
     * 才会调用 {@code getReference} 实例化该 Bean —— 避免实例化无关 Bean（尤其是 Arc
     * synthetic bean，如 {@code CommandLineRuntimeConfig}，其创建需要 InjectionPoint 上下文，
     * 直接 getReference 会抛 "A synthetic injection point was not declared"）。</p>
     */
    private List<MQListener> scanListeners() {
        List<MQListener> listeners = new ArrayList<>();
        for (Bean<?> bean : beanManager.getBeans(Object.class)) {
            // 跳过框架内部 Bean（Ambiguous / Decorator / Interceptor 等）
            if (bean.getBeanClass() == null || bean.getBeanClass() == Object.class) {
                continue;
            }
            // 先反射收集该类中的 @MQEventListener 方法（不实例化 Bean）
            List<Method> annotated = new ArrayList<>();
            collectAnnotatedMethods(bean.getBeanClass(), annotated);
            if (annotated.isEmpty()) {
                continue;
            }
            // 仅对声明了监听器方法的 Bean 实例化（业务 Bean 均可正常创建）
            Object instance;
            try {
                instance = beanManager.getReference(bean, bean.getBeanClass(),
                        beanManager.createCreationalContext(null));
            } catch (Exception e) {
                // synthetic / 特殊 Bean 无法在无 InjectionPoint 上下文创建：跳过并告警
                log.warnf("Skipping bean %s: cannot instantiate for listener scan: %s",
                        bean.getBeanClass().getName(), e.getMessage());
                continue;
            }
            for (Method method : annotated) {
                MQEventListener ann = method.getAnnotation(MQEventListener.class);
                if (ann == null) {
                    continue;
                }
                listeners.add(MQListener.of(instance, method, ann));
                log.debugf("Found @MQEventListener: %s.%s (topic=%s, tags=%s)",
                        bean.getBeanClass().getSimpleName(), method.getName(), ann.topic(), ann.tags());
            }
        }
        return listeners;
    }

    /**
     * 递归收集类、其父类及接口中声明了 {@link MQEventListener} 的方法。
     */
    private void collectAnnotatedMethods(Class<?> clazz, List<Method> annotated) {
        if (clazz == null || clazz == Object.class) {
            return;
        }
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getAnnotation(MQEventListener.class) != null) {
                annotated.add(method);
            }
        }
        // 扫描接口默认方法（接口默认实现中的 @MQEventListener）
        for (Class<?> iface : clazz.getInterfaces()) {
            collectAnnotatedMethods(iface, annotated);
        }
        // 扫描父类
        collectAnnotatedMethods(clazz.getSuperclass(), annotated);
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