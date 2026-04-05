package dk.ashlan.agent.product.api;

import dk.ashlan.agent.product.model.ProductArtifactCollectionResponse;
import dk.ashlan.agent.product.model.ProductConversationDetailResponse;
import dk.ashlan.agent.product.model.ProductConversationSummaryResponse;
import dk.ashlan.agent.product.model.ProductOverviewResponse;
import dk.ashlan.agent.product.model.ProductRunDetailResponse;
import dk.ashlan.agent.product.service.ProductLaneService;
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

@Path("/api/v1/assistants")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Product API", description = "Official product backend contract for the assistant frontend. These are the canonical product list, detail, overview, run, and artifact seams.")
public class ProductLaneResource {
    private static final int MAX_LIST_LIMIT = 100;
    private static final int MAX_OVERVIEW_LIMIT = 20;
    private final ProductLaneService laneService;

    public ProductLaneResource(ProductLaneService laneService) {
        this.laneService = laneService;
    }

    @GET
    @Path("/overview")
    @Operation(
            summary = "Inspect the canonical product overview",
            description = "Official product lane overview for the assistant frontend. This is the product-facing dashboard seam, not a chapter demo or admin-only view."
    )
    @APIResponse(
            responseCode = "200",
            description = "Canonical product overview with conversation, run, failure, and artifact summaries.",
            content = @Content(schema = @Schema(implementation = ProductOverviewResponse.class))
    )
    public ProductOverviewResponse overview(
            @Parameter(description = "Maximum number of recent items to include in the overview.")
            @QueryParam("recentLimit") @DefaultValue("5") int recentLimit
    ) {
        validateLimit(recentLimit, MAX_OVERVIEW_LIMIT, "recent");
        return laneService.overview(recentLimit);
    }

    @GET
    @Path("/conversations")
    @Operation(
            summary = "List canonical product conversations",
            description = "Official product list seam for assistant conversations and product timeline cards."
    )
    @APIResponse(
            responseCode = "200",
            description = "Canonical product conversation summaries.",
            content = @Content(schema = @Schema(implementation = ProductConversationSummaryResponse.class, type = SchemaType.ARRAY))
    )
    public List<ProductConversationSummaryResponse> conversations(
            @Parameter(description = "Maximum number of conversations to return.")
            @QueryParam("limit") @DefaultValue("20") int limit
    ) {
        validateLimit(limit, MAX_LIST_LIMIT, "list");
        return laneService.listConversations(limit);
    }

    @GET
    @Path("/conversations/{conversationId}")
    @Operation(
            summary = "Inspect one canonical product conversation",
            description = "Official product detail seam for one conversation. This is the product-facing detail model, not the operator admin contract."
    )
    @APIResponse(
            responseCode = "200",
            description = "Detailed canonical product conversation state.",
            content = @Content(schema = @Schema(implementation = ProductConversationDetailResponse.class))
    )
    @APIResponse(
            responseCode = "404",
            description = "No product conversation exists for the requested identifier.",
            content = @Content(schema = @Schema(implementation = ProductApiErrorResponse.class))
    )
    public ProductConversationDetailResponse conversation(
            @Parameter(description = "Product conversation identifier.", required = true)
            @PathParam("conversationId") String conversationId
    ) {
        return laneService.conversation(conversationId);
    }

    @GET
    @Path("/runs/{runId}")
    @Operation(
            summary = "Inspect one canonical product run",
            description = "Official product run detail seam for a concrete assistant execution. This is the canonical frontend-friendly run model."
    )
    @APIResponse(
            responseCode = "200",
            description = "Detailed canonical product run state.",
            content = @Content(schema = @Schema(implementation = ProductRunDetailResponse.class))
    )
    @APIResponse(
            responseCode = "404",
            description = "No product run exists for the requested identifier.",
            content = @Content(schema = @Schema(implementation = ProductApiErrorResponse.class))
    )
    public ProductRunDetailResponse run(
            @Parameter(description = "Product run identifier.", required = true)
            @PathParam("runId") String runId
    ) {
        return laneService.run(runId);
    }

    @GET
    @Path("/runs/{runId}/artifacts")
    @Operation(
            summary = "Inspect canonical product artifacts for a run",
            description = "Official product artifact seam for a concrete assistant execution. Frontends can use this to render source citations, summaries, and trace previews without touching internal runtime seams."
    )
    @APIResponse(
            responseCode = "200",
            description = "Artifacts associated with the requested product run.",
            content = @Content(schema = @Schema(implementation = ProductArtifactCollectionResponse.class))
    )
    @APIResponse(
            responseCode = "404",
            description = "No product run exists for the requested identifier.",
            content = @Content(schema = @Schema(implementation = ProductApiErrorResponse.class))
    )
    public ProductArtifactCollectionResponse runArtifacts(
            @Parameter(description = "Product run identifier.", required = true)
            @PathParam("runId") String runId
    ) {
        return laneService.runArtifacts(runId);
    }

    private static void validateLimit(int limit, int max, String fieldName) {
        if (limit < 1 || limit > max) {
            throw new ProductApiException(400, "product_" + fieldName + "_out_of_range", fieldName + " must be between 1 and " + max, null, null, null);
        }
    }
}
