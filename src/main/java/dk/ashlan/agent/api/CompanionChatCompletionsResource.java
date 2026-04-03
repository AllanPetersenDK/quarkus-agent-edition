package dk.ashlan.agent.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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

import java.util.List;

@Path("/api/companion/llm")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Chapter 02 Companion Debug", description = "Swagger-visible chapter-02 companion/debug seam for direct chat-completions-style requests.")
public class CompanionChatCompletionsResource {
    private final CompanionChatCompletionsService service;

    public CompanionChatCompletionsResource(CompanionChatCompletionsService service) {
        this.service = service;
    }

    @POST
    @Path("/completions")
    @Operation(
            summary = "Simulate a unified chat completion",
            description = "Book chapter: 2. Companion/debug seam that simulates a direct LLM API call via Swagger UI using a LiteLLM-style `model` + `messages[]` request. This is a companion seam, not the main manual agent runtime API."
    )
    @RequestBody(
            description = "LiteLLM-style unified chat-completions request payload for the companion/debug seam.",
            required = true,
            content = @Content(schema = @Schema(implementation = CompanionChatCompletionRequest.class))
    )
    @APIResponse(
            responseCode = "200",
            description = "Minimal OpenAI/LiteLLM-style companion response.",
            content = @Content(schema = @Schema(implementation = CompanionChatCompletionResponse.class))
    )
    public CompanionChatCompletionResponse complete(@Valid CompanionChatCompletionRequest request) {
        return service.complete(request);
    }

    public record CompanionChatCompletionRequest(
            @Schema(
                    description = "Requested model label. Defaults to the repo's configured OpenAI model when omitted.",
                    example = "gpt-4.1-mini"
            )
            String model,
            @NotEmpty
            @Valid
            @Schema(
                    description = "Unified chat messages sent to the companion/debug seam.",
                    required = true
            )
            List<ChatMessage> messages,
            @Schema(
                    description = "Optional generation temperature. Accepted for parity with unified chat-completions requests, but this companion seam does not promise per-request provider tuning.",
                    example = "0.0"
            )
            Double temperature,
            @Schema(
                    description = "Optional max token hint for parity with unified chat-completions requests.",
                    example = "256"
            )
            Integer maxTokens
    ) {
        public record ChatMessage(
                @NotBlank
                @Schema(description = "Message role.", example = "user", required = true)
                String role,
                @NotBlank
                @Schema(description = "Message content.", example = "What is the capital of France?", required = true)
                String content
        ) {
        }
    }

    public record CompanionChatCompletionResponse(
            @Schema(description = "Requested or defaulted model label echoed back by the companion seam.")
            String model,
            @Schema(description = "OpenAI-style choices array with a single assistant message.")
            List<Choice> choices,
            @Schema(description = "Indicator that makes the companion/debug seam explicit in Swagger responses.")
            String providerPath
    ) {
        public record Choice(
                @Schema(description = "Choice index.")
                int index,
                @Schema(description = "Assistant message returned by the companion seam.")
                Message message
        ) {
        }

        public record Message(
                @Schema(description = "Assistant role.")
                String role,
                @Schema(description = "Assistant message content.")
                String content
        ) {
        }
    }
}
