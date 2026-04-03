package dk.ashlan.agent.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/workflow-demo")
@Produces(MediaType.TEXT_PLAIN)
@Tag(name = "Internal Chapter Demo", description = "HTTP-exposed chapter demo endpoint for a deterministic workflow comparison.")
public class WorkflowResource {
    @GET
    @Operation(
            summary = "Run the workflow demo",
            description = "Book chapter: 7. Internal chapter demo endpoint for the deterministic workflow comparison."
    )
    @APIResponse(responseCode = "200", description = "Plain-text workflow demo result.")
    public String demo() {
        return "Workflow demo: a deterministic workflow solves the task step by step without autonomous tool use.";
    }
}
