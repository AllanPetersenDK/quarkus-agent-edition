package dk.ashlan.agent.api;

import dk.ashlan.agent.eval.gaia.GaiaCaseResult;
import dk.ashlan.agent.eval.gaia.GaiaRunRequest;
import dk.ashlan.agent.eval.gaia.GaiaRunResult;
import dk.ashlan.agent.eval.gaia.GaiaValidationRunner;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/admin/evaluations/gaia")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "GAIA Validation", description = "GAIA validation/dev admin seam for validation runs, per-task lookup, and run history.")
public class GaiaEvaluationResource {
    private final GaiaValidationRunner runner;

    public GaiaEvaluationResource(GaiaValidationRunner runner) {
        this.runner = runner;
    }

    @POST
    @Path("/run")
    @Operation(
            summary = "Run GAIA validation",
            description = "Book chapter: 10. Dedicated GAIA validation/dev seam that loads a real GAIA validation snapshot, resolves attachments as trace/context, and runs the manual runtime agent on the selected subset."
    )
    @RequestBody(
            required = true,
            content = @Content(schema = @Schema(implementation = GaiaRunRequest.class))
    )
    @APIResponse(
            responseCode = "200",
            description = "GAIA validation run summary and per-case results.",
            content = @Content(schema = @Schema(implementation = GaiaRunResult.class))
    )
    public GaiaRunResult run(GaiaRunRequest request) {
        return runner.run(request);
    }

    @GET
    @Path("/{taskId}")
    @Operation(
            summary = "Inspect GAIA task result",
            description = "Book chapter: 10. Read-only lookup for the most recent GAIA case result for a taskId."
    )
    @APIResponse(
            responseCode = "200",
            description = "Stored GAIA case result.",
            content = @Content(schema = @Schema(implementation = GaiaCaseResult.class))
    )
    @APIResponse(responseCode = "404", description = "No GAIA result exists for the requested task id.")
    public GaiaCaseResult task(
            @Parameter(description = "GAIA task identifier.", required = true)
            @PathParam("taskId") String taskId
    ) {
        try {
            return runner.trace(taskId);
        } catch (IllegalArgumentException exception) {
            throw new NotFoundException(exception.getMessage());
        }
    }

    @GET
    @Path("/runs/{runId}")
    @Operation(
            summary = "Inspect GAIA run result",
            description = "Book chapter: 10. Read-only lookup for a previously executed GAIA validation run."
    )
    @APIResponse(
            responseCode = "200",
            description = "Stored GAIA run result.",
            content = @Content(schema = @Schema(implementation = GaiaRunResult.class))
    )
    @APIResponse(responseCode = "404", description = "No GAIA run exists for the requested run id.")
    public GaiaRunResult runLookup(
            @Parameter(description = "GAIA run identifier.", required = true)
            @PathParam("runId") String runId
    ) {
        try {
            return runner.runResult(runId);
        } catch (IllegalArgumentException exception) {
            throw new NotFoundException(exception.getMessage());
        }
    }
}
