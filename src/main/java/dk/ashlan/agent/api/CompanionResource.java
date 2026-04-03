package dk.ashlan.agent.api;

import dk.ashlan.agent.chapters.chapter07.companion.LangChain4jAgenticCompanionDemo;
import dk.ashlan.agent.llm.LangChain4jCompanionAssistant;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/companion/langchain4j")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Companion API", description = "Swagger-visible LangChain4j comparison seams for the companion runtime.")
public class CompanionResource {
    private final LangChain4jCompanionAssistant assistant;
    private final LangChain4jAgenticCompanionDemo agenticDemo;

    public CompanionResource(LangChain4jCompanionAssistant assistant, LangChain4jAgenticCompanionDemo agenticDemo) {
        this.assistant = assistant;
        this.agenticDemo = agenticDemo;
    }

    @POST
    @Path("/run")
    @Operation(
            summary = "Run the LangChain4j companion seam",
            description = "Framework-backed companion comparison seam that answers a single prompt via LangChain4j. It is exposed for comparison with the manual runtime, not as a replacement for it."
    )
    @RequestBody(
            description = "Prompt to send to the framework-backed companion assistant.",
            required = true,
            content = @Content(schema = @Schema(implementation = CompanionRunRequest.class))
    )
    @APIResponse(
            responseCode = "200",
            description = "Framework-backed companion answer.",
            content = @Content(schema = @Schema(implementation = CompanionRunResponse.class))
    )
    public CompanionRunResponse run(@Valid CompanionRunRequest request) {
        return new CompanionRunResponse(assistant.answer(request.prompt()), "LangChain4j companion seam");
    }

    @POST
    @Path("/agentic-demo")
    @Operation(
            summary = "Run the LangChain4j agentic demo",
            description = "Framework-backed agentic comparison seam that runs the chapter 07 planning workflow over HTTP. It stays separate from the manual orchestrator and is exposed only as a comparison path."
    )
    @RequestBody(
            description = "Topic to send into the agentic companion workflow.",
            required = true,
            content = @Content(schema = @Schema(implementation = CompanionDemoRequest.class))
    )
    @APIResponse(
            responseCode = "200",
            description = "Framework-backed agentic demo output.",
            content = @Content(schema = @Schema(implementation = CompanionDemoResponse.class))
    )
    public CompanionDemoResponse agenticDemo(@Valid CompanionDemoRequest request) {
        return new CompanionDemoResponse(agenticDemo.run(request.topic()), "LangChain4j agentic demo");
    }

    public record CompanionRunRequest(
            @NotBlank
            @Schema(description = "Prompt to send to the companion assistant.", required = true)
            String prompt
    ) {
    }

    public record CompanionRunResponse(
            @Schema(description = "Answer returned by the framework-backed companion seam.")
            String answer,
            @Schema(description = "Label that makes the seam explicit in Swagger responses.")
            String seam
    ) {
    }

    public record CompanionDemoRequest(
            @NotBlank
            @Schema(description = "Topic for the agentic companion workflow.", required = true)
            String topic
    ) {
    }

    public record CompanionDemoResponse(
            @Schema(description = "Plan or demo output returned by the agentic companion flow.")
            String result,
            @Schema(description = "Label that makes the seam explicit in Swagger responses.")
            String seam
    ) {
    }
}
