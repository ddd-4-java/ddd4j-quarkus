package io.ddd4j.quarkus.sample.cqrs.person;

import io.ddd4j.core.cqrs.projection.DefaultProjectionService;
import io.ddd4j.core.cqrs.projection.InMemoryProjectionPositionRepository;
import io.ddd4j.core.cqrs.projection.ProjectionRunner;
import io.ddd4j.sample.cqrs.person.application.PersonCommandService;
import io.ddd4j.sample.cqrs.person.domain.CreatePersonCommand;
import io.ddd4j.sample.cqrs.person.domain.PersonEvent;
import io.ddd4j.sample.cqrs.person.infrastructure.InMemoryPersonEventStore;
import io.ddd4j.sample.cqrs.person.infrastructure.InMemoryPersonRepository;
import io.ddd4j.sample.cqrs.person.query.PersonListView;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuarkusPersonResourceTest {

    @Test
    void shouldRunPersonCqrsFlow() {
        InMemoryPersonEventStore eventStore = new InMemoryPersonEventStore();
        PersonCommandService commandService = new PersonCommandService(new InMemoryPersonRepository(eventStore));
        PersonListView view = new PersonListView();
        ProjectionRunner<PersonEvent> runner = new ProjectionRunner<>(
                new DefaultProjectionService(new InMemoryProjectionPositionRepository()),
                eventStore
        );
        QuarkusPersonResource resource = new QuarkusPersonResource(commandService, view, runner);

        assertEquals("p-100", resource.create(CreatePersonCommand.builder()
                .personId("p-100")
                .name("Alice")
                .build()).get("personId"));
        assertEquals("Alice", resource.get("p-100").getName());
        assertEquals(1, resource.all().size());

        assertTrue(resource.delete("p-100").get("deleted"));
        assertNull(resource.get("p-100"));
    }
}
