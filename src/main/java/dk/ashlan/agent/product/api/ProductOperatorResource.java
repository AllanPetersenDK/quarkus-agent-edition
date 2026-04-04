package dk.ashlan.agent.product.api;

import dk.ashlan.agent.product.model.ProductConversationDetailResponse;
import dk.ashlan.agent.product.model.ProductConversationSummaryResponse;
import dk.ashlan.agent.product.store.ProductConversationStore;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/api/v1/assistants/admin")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Product Operator", description = "Lightweight operator seam for inspecting product conversations and their persistent state.")
public class ProductOperatorResource {
    private final ProductConversationStore conversationStore;

    public ProductOperatorResource(ProductConversationStore conversationStore) {
        this.conversationStore = conversationStore;
    }

    @GET
    @Path("/conversations")
    @Operation(
            summary = "List product conversations",
            description = "Phase-2 product operator seam that lists the persistent product conversations and their latest quality signals."
    )
    @APIResponse(
            responseCode = "200",
            description = "Product conversation summaries.",
            content = @Content(schema = @Schema(implementation = ProductConversationSummaryResponse.class, type = SchemaType.ARRAY))
    )
    public List<ProductConversationSummaryResponse> listConversations(
            @Parameter(description = "Maximum number of summaries to return.")
            @QueryParam("limit") @DefaultValue("20") int limit
    ) {
        return conversationStore.list(limit).stream()
                .map(ProductConversationSummaryResponse::from)
                .toList();
    }

    @GET
    @Path("/conversations/{conversationId}")
    @Operation(
            summary = "Inspect a product conversation",
            description = "Phase-2 product operator seam that returns the persistent state for one product conversation."
    )
    @APIResponse(
            responseCode = "200",
            description = "Detailed product conversation state.",
            content = @Content(schema = @Schema(implementation = ProductConversationDetailResponse.class))
    )
    @APIResponse(responseCode = "404", description = "No product conversation exists for the requested identifier.")
    public ProductConversationDetailResponse conversation(
            @Parameter(description = "Product conversation identifier.", required = true)
            @PathParam("conversationId") String conversationId
    ) {
        return conversationStore.load(conversationId)
                .map(ProductConversationDetailResponse::from)
                .orElseThrow(() -> new jakarta.ws.rs.NotFoundException("No product conversation found for conversationId=" + conversationId));
    }
}
