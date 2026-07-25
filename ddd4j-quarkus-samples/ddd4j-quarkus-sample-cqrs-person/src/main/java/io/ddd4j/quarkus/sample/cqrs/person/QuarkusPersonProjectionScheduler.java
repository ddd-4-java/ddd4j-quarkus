package io.ddd4j.quarkus.sample.cqrs.person;

import io.ddd4j.core.cqrs.readmodel.ProjectionRunner;
import io.ddd4j.sample.cqrs.person.domain.PersonEvent;
import io.ddd4j.sample.cqrs.person.query.PersonListView;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class QuarkusPersonProjectionScheduler {

    private final PersonListView view;

    private final ProjectionRunner<PersonEvent> runner;

    @Inject
    public QuarkusPersonProjectionScheduler(PersonListView view, ProjectionRunner<PersonEvent> runner) {
        this.view = view;
        this.runner = runner;
    }

    @Scheduled(every = "5s")
    public void runOnce() {
        runner.runOnce(view);
    }
}
