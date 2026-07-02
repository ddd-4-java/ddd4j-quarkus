package io.ddd4j.quarkus.sample.richmodel.order;

import io.ddd4j.sample.richmodel.order.application.OrderApplicationService;
import io.ddd4j.sample.richmodel.order.domain.repository.OrderRepository;
import io.ddd4j.sample.richmodel.order.infrastructure.persistence.InMemoryOrderRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * CDI wiring for the pure Java rich-model sample.
 */
@ApplicationScoped
public class RichModelOrderProducer {

    @Produces
    @Singleton
    public OrderRepository orderRepository() {
        return new InMemoryOrderRepository();
    }

    @Produces
    @Singleton
    public OrderApplicationService orderApplicationService(OrderRepository repository) {
        return new OrderApplicationService(repository);
    }
}
