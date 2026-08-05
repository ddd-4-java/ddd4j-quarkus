package io.ddd4j.quarkus.sample.infrastructure.order.config;

import io.ddd4j.core.ddd.repository.RepositoryRegistry;
import io.ddd4j.quarkus.sample.domain.order.model.aggregate.Order;
import io.ddd4j.quarkus.sample.domain.order.repository.OrderRepository;
import io.ddd4j.quarkus.sample.infrastructure.order.persistence.OrderRepositoryImpl;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.jboss.logging.Logger;

/**
 * 订单基础设施装配配置（Quarkus CDI）。
 *
 * <p>通过 {@link Produces} 将 {@link OrderRepositoryImpl} 装配为
 * {@link OrderRepository} CDI Bean，并注册到
 * {@link RepositoryRegistry#register(Class, io.ddd4j.core.ddd.repository.Repository)}，
 * 使聚合根充血持久化（{@code order.save() / order.update()}）与充血查询
 * （{@code AggregateRoot.get/list/page(...)}）可在运行时自动定位仓储实例。</p>
 *
 * <p>与 ddd4j-quarkus-sample-rich-model 的
 * {@code RichModelOrderProducer} 装配模式保持一致。</p>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@ApplicationScoped
public class OrderInfrastructureConfig {

    private static final Logger log = Logger.getLogger(OrderInfrastructureConfig.class);

    /**
     * 生产订单仓储 Bean，并注册到 {@link RepositoryRegistry}。
     *
     * @return 订单仓储实现
     */
    @Produces
    @Singleton
    public OrderRepository orderRepository() {
        OrderRepositoryImpl repository = new OrderRepositoryImpl();
        // 注册到 ddd4j 仓储注册表：Order 聚合根充血持久化 / 查询通过它定位仓储
        RepositoryRegistry.register(Order.class, repository);
        log.infof("[OrderInfrastructureConfig] OrderRepository registered for aggregate %s", Order.class.getName());
        return repository;
    }
}
