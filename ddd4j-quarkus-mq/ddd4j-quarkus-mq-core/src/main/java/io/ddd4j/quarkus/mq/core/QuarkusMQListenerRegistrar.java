package io.ddd4j.quarkus.mq.core;

import io.ddd4j.mq.annotation.MQEventListener;
import io.ddd4j.mq.ack.AckDisposition;
import io.ddd4j.mq.ack.MessageAcknowledgment;
import io.ddd4j.mq.ack.MQConsumeTemplates;
import io.ddd4j.mq.ack.NoOpMessageAcknowledgment;
import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.consume.MQConsumerContext;
import io.ddd4j.mq.consume.MQConsumerHandler;
import io.ddd4j.mq.consume.MQConsumeInterceptor;
import io.ddd4j.mq.contract.MQMessage;
import io.ddd4j.mq.registry.MQBrokerType;
import io.ddd4j.mq.registry.MQListenerDefinition;
import io.ddd4j.mq.registry.MQListenerMethodInvoker;
import io.ddd4j.mq.serialization.MQEventSerialization;
import io.ddd4j.mq.spi.MQBrokerAdapter;
import io.ddd4j.mq.spi.MQBrokerAdapters;
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
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Quarkus MQ 监听器注册器：应用启动时通过 CDI {@link BeanManager} 扫描所有带
 * {@link MQEventListener} 注解方法的 Bean，构建 {@link MQListenerDefinition}，
 * 并委托 {@link MQBrokerAdapter} 注册消费端点。
 *
 * <p>对标 ddd4j-mq-spring 的 {@code MQListenerRegistrar}，消息处理流程一致：
 * <pre>
 *   Broker 原生消息
 *     ↓ Adapter 内部转为纯 Java MQMessage
 *   MQConsumerHandler.handle(message, ack)
 *     ↓ MQConsumeTemplates.execute（拦截链 + 幂等模板）
 *   MQListenerMethodInvoker.invoke → 反射调用 @MQEventListener 方法
 *   业务方法
 * </pre>
 *
 * <p>与 Spring 版的区别：用 {@link BeanManager#getBeans(Object)} + {@code Bean#create()}
 * 替代 Spring 的 BeanPostProcessor 扫描；用 {@code @Observes StartupEvent} 替代
 * {@code ContextRefreshedEvent}。
 *
 * <p>当无 {@link MQBrokerAdapter} 实现时，扫描结果仅记录日志不注册（便于无 MQ 场景下安全启动）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @see MQBrokerAdapter
 * @since 3.3.x
 */
@ApplicationScoped
public class QuarkusMQListenerRegistrar {

    private static final Logger logger = Logger.getLogger(QuarkusMQListenerRegistrar.class);

    @Inject
    BeanManager beanManager;

    @Inject
    Instance<MQBrokerAdapter> adapters;

    @Inject
    Ddd4jMQProperties properties;

    @Inject
    MQEventSerialization serialization;

    @Inject
    Instance<MQConsumeInterceptor> interceptors;

    /**
     * 应用启动时扫描并注册所有 MQ 监听器。
     *
     * @param event Quarkus 启动事件
     */
    void onStart(@Observes StartupEvent event) {
        List<MQListenerDefinition> definitions = scanDefinitions();
        if (definitions.isEmpty()) {
            logger.debug("No @MQEventListener definitions to register");
            return;
        }
        if (adapters.isUnsatisfied()) {
            logger.warnf("Found %d @MQEventListener(s) but no MQBrokerAdapter available; listeners will NOT be registered. "
                    + "Add a broker module (e.g. ddd4j-quarkus-mq-kafka) to enable MQ consumption.", definitions.size());
            return;
        }

        MQBrokerAdapter adapter = MQBrokerAdapters.selectAdapter(iterableToList(adapters), properties);
        MQListenerMethodInvoker invoker = new MQListenerMethodInvoker(serialization);
        List<MQConsumeInterceptor> orderedInterceptors = orderedInterceptors();

        int registered = 0;
        for (MQListenerDefinition definition : definitions) {
            try {
                MQConsumerHandler handler = createHandler(definition, adapter, invoker, orderedInterceptors);
                adapter.registerConsumer(definition, handler);
                registered++;
                logger.infof("Registered MQ listener: bean=%s, method=%s, topic=%s, group=%s",
                        definition.getBean().getClass().getSimpleName(),
                        definition.getMethod().getName(),
                        definition.getTopic(),
                        definition.getGroup());
            } catch (Exception e) {
                logger.errorf(e, "Failed to register MQ listener: bean=%s, method=%s",
                        definition.getBean().getClass().getSimpleName(),
                        definition.getMethod().getName());
            }
        }
        logger.infof("MQ listener registration completed: %d/%d registered", registered, definitions.size());
    }

    /**
     * 通过 BeanManager 扫描所有 CDI Bean 中带 {@link MQEventListener} 注解的方法。
     *
     * @return 监听器定义列表
     */
    private List<MQListenerDefinition> scanDefinitions() {
        List<MQListenerDefinition> definitions = new ArrayList<>();
        Set<Bean<?>> beans = beanManager.getBeans(Object.class);
        for (Bean<?> bean : beans) {
            Class<?> beanClass = bean.getBeanClass();
            if (Objects.isNull(beanClass) || beanClass.isSynthetic()) {
                continue;
            }
            for (Method method : beanClass.getDeclaredMethods()) {
                MQEventListener ann = method.getAnnotation(MQEventListener.class);
                if (Objects.nonNull(ann)) {
                    Object instance = resolveBeanInstance(bean);
                    if (Objects.nonNull(instance)) {
                        definitions.add(MQListenerDefinition.from(instance, method, ann));
                    }
                }
            }
        }
        return definitions;
    }

    /**
     * 解析 Bean 实例（通过 BeanManager 的参考）。
     */
    @SuppressWarnings("unchecked")
    private Object resolveBeanInstance(Bean<?> bean) {
        try {
            return beanManager.getReference((Bean<Object>) bean, bean.getBeanClass(), beanManager.createCreationalContext(null));
        } catch (Exception e) {
            logger.warnf(e, "Failed to resolve bean instance for %s", bean.getBeanClass());
            return null;
        }
    }

    /**
     * 为单个监听器定义创建默认消费处理器（与 Spring 版逻辑一致）。
     */
    private MQConsumerHandler createHandler(
            MQListenerDefinition definition,
            MQBrokerAdapter adapter,
            MQListenerMethodInvoker invoker,
            List<MQConsumeInterceptor> orderedInterceptors) {

        return (message, ack) -> {
            MessageAcknowledgment effectiveAck = resolveAcknowledgment(adapter, message, ack);
            MQConsumerContext context = invoker.buildContext(definition, message, effectiveAck);
            AtomicReference<AckDisposition> dispositionRef = new AtomicReference<>();
            try {
                MQConsumeTemplates.execute(
                        message,
                        effectiveAck,
                        () -> runPreCheck(orderedInterceptors, context, message),
                        () -> {
                            try {
                                AckDisposition disposition = invoker.invoke(definition, context, message);
                                dispositionRef.set(disposition);
                                return disposition;
                            } catch (Exception ex) {
                                logger.errorf(ex, "MQ listener invocation failed: bean=%s, method=%s",
                                        definition.getBean().getClass().getSimpleName(),
                                        definition.getMethod().getName());
                                throw new RuntimeException(ex);
                            }
                        });
            } catch (Exception ex) {
                if (Objects.nonNull(properties.getConsumer())
                        && properties.getConsumer().isManualAck()
                        && !effectiveAck.isAcknowledged()) {
                    effectiveAck.requeue();
                }
            } finally {
                runAfterConsume(orderedInterceptors, context, message, dispositionRef.get());
                invoker.clearContext();
            }
        };
    }

    /**
     * 解析确认端口：优先 Adapter 从 nativeMessage 解析，回退传入 ack 或 NoOp。
     */
    private MessageAcknowledgment resolveAcknowledgment(
            MQBrokerAdapter adapter,
            MQMessage<?> message,
            MessageAcknowledgment ack) {
        MessageAcknowledgment resolved = adapter.resolveAcknowledgment(message);
        if (Objects.nonNull(resolved)) {
            return resolved;
        }
        return Objects.nonNull(ack) ? ack : new NoOpMessageAcknowledgment();
    }

    /**
     * 执行拦截链 preCheck，返回首个非零结果。
     */
    private int runPreCheck(
            List<MQConsumeInterceptor> orderedInterceptors,
            MQConsumerContext context,
            MQMessage<?> message) {
        for (MQConsumeInterceptor interceptor : orderedInterceptors) {
            int result = interceptor.preCheck(context, message);
            if (result != MQConsumeTemplates.PRE_CONTINUE) {
                return result;
            }
        }
        return MQConsumeTemplates.PRE_CONTINUE;
    }

    /**
     * 执行拦截链 afterConsume。
     */
    private void runAfterConsume(
            List<MQConsumeInterceptor> orderedInterceptors,
            MQConsumerContext context,
            MQMessage<?> message,
            AckDisposition disposition) {
        for (MQConsumeInterceptor interceptor : orderedInterceptors) {
            try {
                interceptor.afterConsume(context, message, disposition);
            } catch (Exception e) {
                logger.warnf(e, "MQ interceptor afterConsume failed: %s", interceptor.getClass().getSimpleName());
            }
        }
    }

    /**
     * 按优先级排序的拦截器列表。
     */
    private List<MQConsumeInterceptor> orderedInterceptors() {
        List<MQConsumeInterceptor> list = iterableToList(interceptors);
        list.sort(Comparator.comparingInt(MQConsumeInterceptor::order));
        return list;
    }

    /**
     * CDI Instance 转 List。
     */
    private <T> List<T> iterableToList(Instance<T> instance) {
        List<T> list = new ArrayList<>();
        for (T item : instance) {
            list.add(item);
        }
        return list;
    }
}
