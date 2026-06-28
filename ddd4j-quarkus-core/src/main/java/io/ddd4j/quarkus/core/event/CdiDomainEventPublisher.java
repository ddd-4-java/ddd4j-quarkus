package io.ddd4j.quarkus.core.event;

import io.ddd4j.core.contract.DomainEvent;
import io.ddd4j.core.contract.DomainEventPublisher;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.Collection;

/**
 * CDI 实现的领域事件发布者
 * <p>
 * 使用 CDI Event&lt;DomainEvent&gt; 机制发布领域事件，
 * 替代 Spring 的 ApplicationEventPublisher。
 *
 * @author Loong Wan
 */
@ApplicationScoped
public class CdiDomainEventPublisher implements DomainEventPublisher {

    private static final Logger logger = Logger.getLogger(CdiDomainEventPublisher.class);

    @Inject
    Event<DomainEvent> eventBus;

    @Override
    public void publish(DomainEvent event) {
        if (event == null) {
            logger.warn("Attempted to publish null domain event");
            return;
        }
        logger.debugf("Publishing domain event: %s, aggregateId: %s", event.getEventType(), event.getAggregateId());
        eventBus.fire(event);
    }

    @Override
    public void publishAll(Collection<DomainEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        logger.debugf("Publishing %d domain events", events.size());
        for (DomainEvent event : events) {
            publish(event);
        }
    }
}
