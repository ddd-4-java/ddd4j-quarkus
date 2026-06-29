package io.ddd4j.quarkus.ddd.config;

import io.ddd4j.core.ddd.config.DddProperties;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.fuin.esc.api.EventStore;
import org.fuin.esc.mem.InMemoryEventStore;
import org.jboss.logging.Logger;

/**
 * Quarkus EventStore 自动配置。
 *
 * <p>根据 {@link DddProperties} 配置自动创建 EventStore 实例：
 * <ul>
 *   <li>{@code mem} — 内存版（默认，开发/测试用）</li>
 *   <li>{@code kurrent} — KurrentDB/EventStoreDB（生产环境，需自行注入 EventStore Bean）</li>
 * </ul>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 2.0.x
 */
@ApplicationScoped
public class Ddd4jEventStoreConfig {

    private static final Logger logger = Logger.getLogger(Ddd4jEventStoreConfig.class);

    @Inject
    DddProperties dddProperties;

    /**
     * 生产内存版 EventStore（开发/测试用）。
     *
     * <p>生产环境应替换为 KurrentDB/EventStoreDB 实现：
     * <pre>
     * &#64;Produces
     * &#64;Singleton
     * public EventStore eventStore() {
     *     return new KurrentDbEventStore(...);
     * }
     * </pre>
     */
    @Produces
    @Singleton
    @ApplicationScoped
    public EventStore eventStore() {
        String type = dddProperties.getEventStore().getType();
        logger.infof("Creating EventStore with type: %s", type);

        if ("mem".equals(type)) {
            logger.info("Using in-memory EventStore (development/test mode)");
            return new InMemoryEventStore();
        }

        // 生产环境：KurrentDB/EventStoreDB
        // 业务项目应自行注入 EventStore Bean
        logger.warnf("EventStore type '%s' not supported by auto-configuration. " +
                "Please provide your own EventStore bean.", type);
        return new InMemoryEventStore();
    }

    void onStart(@Observes StartupEvent event) {
        logger.infof("Ddd4jEventStoreConfig initialized with type: %s",
                dddProperties.getEventStore().getType());
    }
}
