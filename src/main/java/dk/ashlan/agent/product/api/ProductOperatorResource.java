package dk.ashlan.agent.product.api;

import dk.ashlan.agent.product.model.ProductConversationDetailResponse;
import dk.ashlan.agent.product.model.ProductConversationSummaryResponse;
import dk.ashlan.agent.product.model.ProductOperatorOverviewResponse;
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
@Tag(name = "Product Operator", description = "Secondary operator/admin inspection surface for the canonical product lane. Used for read-only drift checks, release-gate review, and closed-network health inspection.")
public class ProductOperatorResource {
    private static final int MAX_LIST_LIMIT = 100;
    private static final int MAX_OVERVIEW_LIMIT = 20;
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
        validateLimit(limit, MAX_LIST_LIMIT, "list");
        return conversationStore.list(limit).stream()
                .map(ProductConversationSummaryResponse::from)
                .toList();
    }

    @GET
    @Path("/overview")
    @Operation(
            summary = "Inspect the product operator overview",
            description = "Closed-network operator seam that summarizes the product lane, highlights the latest conversation, and exposes a compact drift-readiness signal."
    )
    @APIResponse(
            responseCode = "200",
            description = "Compact product operator overview.",
            content = @Content(schema = @Schema(implementation = ProductOperatorOverviewResponse.class))
    )
    public ProductOperatorOverviewResponse overview(
            @Parameter(description = "Maximum number of recent conversations to include in the overview.")
            @QueryParam("recentLimit") @DefaultValue("5") int recentLimit
    ) {
        validateLimit(recentLimit, MAX_OVERVIEW_LIMIT, "recent");
        List<ProductConversationSummaryResponse> recentConversations = conversationStore.list(recentLimit).stream()
                .map(ProductConversationSummaryResponse::from)
                .toList();
        long conversationCount = conversationStore.count();
        ProductConversationSummaryResponse latest = recentConversations.isEmpty() ? null : recentConversations.get(0);
        return new ProductOperatorOverviewResponse(
                conversationCount,
                recentConversations.size(),
                latest == null ? null : latest.conversationId(),
                latest == null ? null : latest.lastRunId(),
                latest == null ? null : latest.lastStatus(),
                latest == null ? null : latest.updatedAt(),
                latest == null ? null : latest.lastFailureReason(),
                recentConversations,
                List.of(
                        "conversationCount:" + conversationCount,
                        "recentLimit:" + recentLimit,
                        "latestStatus:" + (latest == null ? "none" : latest.lastStatus())
                )
        );
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

    private static void validateLimit(int limit, int max, String fieldName) {
        if (limit < 1 || limit > max) {
            throw new ProductApiException(400, "product_operator_" + fieldName + "_out_of_range", fieldName + " must be between 1 and " + max, null, null, null);
        }
    }
}
