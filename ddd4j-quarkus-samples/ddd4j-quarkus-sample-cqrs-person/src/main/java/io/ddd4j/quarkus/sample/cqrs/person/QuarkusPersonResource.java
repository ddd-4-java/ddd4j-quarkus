package io.ddd4j.quarkus.sample.cqrs.person;

import io.ddd4j.core.cqrs.projection.ProjectionRunner;
import io.ddd4j.sample.cqrs.person.application.PersonCommandService;
import io.ddd4j.sample.cqrs.person.domain.CreatePersonCommand;
import io.ddd4j.sample.cqrs.person.domain.DeletePersonCommand;
import io.ddd4j.sample.cqrs.person.domain.PersonEvent;
import io.ddd4j.sample.cqrs.person.domain.PersonId;
import io.ddd4j.sample.cqrs.person.query.PersonListEntry;
import io.ddd4j.sample.cqrs.person.query.PersonListView;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.Map;

@Path("/persons")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class QuarkusPersonResource {

    private final PersonCommandService commandService;

    private final PersonListView view;

    private final ProjectionRunner<PersonEvent> runner;

    @Inject
    public QuarkusPersonResource(
            PersonCommandService commandService,
            PersonListView view,
            ProjectionRunner<PersonEvent> runner
    ) {
        this.commandService = commandService;
        this.view = view;
        this.runner = runner;
    }

    @POST
    @Path("/create")
    public Map<String, String> create(CreatePersonCommand command) {
        PersonId personId = commandService.create(command);
        runner.runOnce(view);
        return Map.of("personId", personId.getValue());
    }

    @GET
    public List<PersonListEntry> all() {
        runner.runOnce(view);
        return view.findAll();
    }

    @GET
    @Path("/{personId}")
    public PersonListEntry get(@PathParam("personId") String personId) {
        runner.runOnce(view);
        return view.findById(personId).orElse(null);
    }

    @DELETE
    @Path("/{personId}")
    public Map<String, Boolean> delete(@PathParam("personId") String personId) {
        commandService.delete(DeletePersonCommand.builder()
                .personId(personId)
                .build());
        runner.runOnce(view);
        return Map.of("deleted", true);
    }
}
