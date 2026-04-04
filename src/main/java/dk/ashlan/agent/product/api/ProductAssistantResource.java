package dk.ashlan.agent.product.api;

import dk.ashlan.agent.product.model.ProductAssistantQueryRequest;
import dk.ashlan.agent.product.model.ProductAssistantQueryResponse;
import dk.ashlan.agent.product.service.ProductAssistantService;
import jakarta.validation.Valid;
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

@Path("/api/v1/assistants")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Product API", description = "Stable product-lane assistant query seam built on the repo's mature runtime capabilities.")
public class ProductAssistantResource {
    private final ProductAssistantService assistantService;

    public ProductAssistantResource(ProductAssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @POST
    @Path("/query")
    @Operation(
            summary = "Query the product assistant",
            description = "Product lane v1. Small stable query seam built on top of the existing RAG, memory, planning, reflection, and session capabilities without exposing chapter-specific contract details."
    )
    @RequestBody(
            description = "Conversation reference, user query, and optional retrieval depth for the product assistant.",
            required = true,
            content = @Content(schema = @Schema(implementation = ProductAssistantQueryRequest.class))
    )
    @APIResponse(
            responseCode = "200",
            description = "Product assistant response with answer, sources, memory hints, planning, and reflection metadata.",
            content = @Content(schema = @Schema(implementation = ProductAssistantQueryResponse.class))
    )
    public ProductAssistantQueryResponse query(@Valid ProductAssistantQueryRequest request) {
        return assistantService.query(request);
    }
}
