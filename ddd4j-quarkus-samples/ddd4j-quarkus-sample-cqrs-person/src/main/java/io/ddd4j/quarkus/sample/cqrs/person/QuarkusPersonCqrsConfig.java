package io.ddd4j.quarkus.sample.cqrs.person;

import io.ddd4j.core.cqrs.projection.DefaultProjectionService;
import io.ddd4j.core.cqrs.projection.InMemoryProjectionPositionRepository;
import io.ddd4j.core.cqrs.projection.ProjectionRunner;
import io.ddd4j.sample.cqrs.person.application.PersonCommandService;
import io.ddd4j.sample.cqrs.person.domain.PersonEvent;
import io.ddd4j.sample.cqrs.person.infrastructure.InMemoryPersonEventStore;
import io.ddd4j.sample.cqrs.person.infrastructure.InMemoryPersonRepository;
import io.ddd4j.sample.cqrs.person.query.PersonListView;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

@ApplicationScoped
public class QuarkusPersonCqrsConfig {

    @Produces
    @Singleton
    public InMemoryPersonEventStore personEventStore() {
        return new InMemoryPersonEventStore();
    }

    @Produces
    @Singleton
    public InMemoryPersonRepository personRepository(InMemoryPersonEventStore eventStore) {
        return new InMemoryPersonRepository(eventStore);
    }

    @Produces
    @Singleton
    public PersonCommandService personCommandService(InMemoryPersonRepository repository) {
        return new PersonCommandService(repository);
    }

    @Produces
    @Singleton
    public PersonListView personListView() {
        return new PersonListView();
    }

    @Produces
    @Singleton
    public ProjectionRunner<PersonEvent> personProjectionRunner(InMemoryPersonEventStore eventStore) {
        return new ProjectionRunner<>(
                new DefaultProjectionService(new InMemoryProjectionPositionRepository()),
                eventStore
        );
    }
}
