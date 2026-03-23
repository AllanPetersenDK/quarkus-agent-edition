package dk.ashlan.agent.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/workflow-demo")
@Produces(MediaType.TEXT_PLAIN)
public class WorkflowResource {
    @GET
    public String demo() {
        return "Workflow demo: a deterministic workflow solves the task step by step without autonomous tool use.";
    }
}
