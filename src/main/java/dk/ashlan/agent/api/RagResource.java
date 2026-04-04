package dk.ashlan.agent.api;

import dk.ashlan.agent.rag.DocumentChunk;
import dk.ashlan.agent.rag.RagService;
import dk.ashlan.agent.rag.RetrievalResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/api/rag")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "RAG API", description = "REST-exposed knowledge-base query and ingest seams backed by the repo's RAG services.")
public class RagResource {
    private final RagService ragService;

    public RagResource(RagService ragService) {
        this.ragService = ragService;
    }

    @POST
    @Path("/ingest")
    @Operation(
            summary = "Ingest a document into RAG",
            description = "Book chapter: 5. Swagger-visible companion seam that ingests document text into the repo's RAG stack."
    )
    @RequestBody(
            description = "Document source id and content to ingest.",
            required = true,
            content = @Content(schema = @Schema(implementation = RagIngestRequest.class))
    )
    @APIResponse(
            responseCode = "200",
            description = "Chunk summaries created by the ingest.",
            content = @Content(schema = @Schema(implementation = RagIngestResponse.class))
    )
    public RagIngestResponse ingest(@Valid RagIngestRequest request) {
        List<RagChunkResponse> chunks = ragService.ingest(request.sourceId(), request.text()).stream()
                .map(RagChunkResponse::fromIngest)
                .toList();
        return new RagIngestResponse(request.sourceId(), chunks.size(), chunks);
    }

    @GET
    @Path("/query")
    @Operation(
            summary = "Query the RAG stack",
            description = "Book chapter: 5. Swagger-visible RAG query seam that returns the matching chunks and a simple answer."
    )
    @APIResponse(
            responseCode = "200",
            description = "RAG query answer and matched chunk summaries.",
            content = @Content(schema = @Schema(implementation = RagQueryResponse.class))
    )
    @APIResponse(responseCode = "400", description = "Missing or blank query.")
    public RagQueryResponse query(
            @Parameter(description = "Search text used to retrieve relevant knowledge.", required = true)
            @QueryParam("query") String query,
            @Parameter(description = "Maximum number of chunks to return.")
            @QueryParam("topK") @DefaultValue("3") int topK
    ) {
        String effectiveQuery = query == null ? "" : query.trim();
        if (effectiveQuery.isEmpty()) {
            throw new BadRequestException("query is required");
        }
        List<RetrievalResult> results = ragService.retrieve(effectiveQuery, topK);
        List<RagChunkResponse> chunks = results.stream()
                .map(RagChunkResponse::fromQuery)
                .toList();
        String answer = ragService.answer(effectiveQuery, results);
        return new RagQueryResponse(effectiveQuery, answer, chunks);
    }

    public record RagIngestRequest(
            @NotBlank
            @Schema(description = "Identifier for the ingested source document.", required = true, examples = {"chapter-05"})
            String sourceId,
            @NotBlank
            @Schema(description = "Document text to ingest into the RAG stack.", required = true)
            String text
    ) {
    }

    public record RagIngestResponse(
            @Schema(description = "Source identifier used during ingest.")
            String sourceId,
            @Schema(description = "Number of document chunks created by the ingest.")
            int chunkCount,
            @Schema(description = "Chunk summaries written to the RAG stack.")
            List<RagChunkResponse> chunks
    ) {
    }

    public record RagQueryResponse(
            @Schema(description = "Search text used to query the knowledge base.")
            String query,
            @Schema(description = "Simple answer assembled from the matching RAG chunks.")
            String answer,
            @Schema(description = "Matched chunk summaries ordered by relevance.")
            List<RagChunkResponse> chunks
    ) {
    }

    public record RagChunkResponse(
            @Schema(description = "Source document identifier.")
            String sourceId,
            @Schema(description = "Zero-based chunk index within the source document.")
            int chunkIndex,
            @Schema(description = "Chunk text returned by the RAG flow.")
            String text,
            @Schema(description = "Similarity score for query responses. Null for ingest responses.")
            Double similarity
    ) {
        static RagChunkResponse fromIngest(DocumentChunk chunk) {
            return new RagChunkResponse(chunk.sourceId(), chunk.chunkIndex(), chunk.text(), null);
        }

        static RagChunkResponse fromQuery(RetrievalResult result) {
            DocumentChunk chunk = result.chunk();
            return new RagChunkResponse(chunk.sourceId(), chunk.chunkIndex(), chunk.text(), result.similarity());
        }
    }
}
