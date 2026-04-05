package dk.ashlan.agent.api;

import dk.ashlan.agent.rag.DocumentChunk;
import dk.ashlan.agent.rag.RagDirectoryIngestResult;
import dk.ashlan.agent.rag.RagService;
import dk.ashlan.agent.rag.RetrievalResult;
import dk.ashlan.agent.rag.RagService.RagPathIngestResult;
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
import java.util.Map;

@Path("/api/rag")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "RAG API", description = "REST-exposed knowledge-base query and ingest seams backed by the repo's RAG services.")
public class RagResource {
    private static final int MAX_TOP_K = 10;
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

    @POST
    @Path("/ingest/path")
    @Operation(
            summary = "Ingest a workspace document by path",
            description = "Book chapter: 5. Path-based companion seam that resolves a workspace file through the shared document-read layer before ingesting extracted text into RAG."
    )
    @RequestBody(
            description = "Workspace path and optional source id for the document to ingest.",
            required = true,
            content = @Content(schema = @Schema(implementation = RagIngestPathRequest.class))
    )
    @APIResponse(
            responseCode = "200",
            description = "Ingest result, extraction metadata, and chunk summaries.",
            content = @Content(schema = @Schema(implementation = RagIngestPathResponse.class))
    )
    @APIResponse(responseCode = "400", description = "Missing or blank path.")
    public RagIngestPathResponse ingestPath(@Valid RagIngestPathRequest request) {
        RagPathIngestResult result = ragService.ingestPath(request.path(), request.sourceId());
        return RagIngestPathResponse.from(result);
    }

    @POST
    @Path("/ingest/directory")
    @Operation(
            summary = "Ingest a workspace directory into RAG",
            description = "Book chapter: 5. Bulk ingest companion seam that walks a workspace directory, reads supported files through the shared document layer, and ingests the successful documents into RAG."
    )
    @RequestBody(
            description = "Workspace directory and options for bulk ingest.",
            required = true,
            content = @Content(schema = @Schema(implementation = RagDirectoryIngestRequest.class))
    )
    @APIResponse(
            responseCode = "200",
            description = "Directory ingest summary with per-file results.",
            content = @Content(schema = @Schema(implementation = RagDirectoryIngestResult.class))
    )
    @APIResponse(responseCode = "400", description = "Missing or blank path.")
    public RagDirectoryIngestResult ingestDirectory(@Valid RagDirectoryIngestRequest request) {
        boolean recursive = request.recursive() != null && request.recursive();
        int maxFiles = request.maxFiles() == null ? 20 : request.maxFiles();
        return ragService.ingestDirectory(request.path(), request.sourceIdPrefix(), recursive, maxFiles);
    }

    @GET
    @Path("/query")
    @Operation(
            summary = "Query the RAG stack",
            description = "Book chapter: 5. Swagger-visible RAG query seam that returns the matching chunks, compact citations, and a simple answer."
    )
    @APIResponse(
            responseCode = "200",
            description = "RAG query answer, best chunk, compact citations, and matched chunk summaries.",
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
        if (topK < 1 || topK > MAX_TOP_K) {
            throw new BadRequestException("topK must be between 1 and " + MAX_TOP_K);
        }
        List<RetrievalResult> results = ragService.retrieve(effectiveQuery, topK);
        List<RagChunkResponse> chunks = results.stream()
                .map(RagChunkResponse::fromQuery)
                .toList();
        RagChunkResponse bestChunk = chunks.isEmpty() ? null : chunks.get(0);
        List<RagCitationResponse> citations = results.stream()
                .map(RagCitationResponse::fromQuery)
                .toList();
        String answer = ragService.answer(effectiveQuery, results);
        return new RagQueryResponse(effectiveQuery, answer, bestChunk, citations, chunks);
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

    public record RagIngestPathRequest(
            @NotBlank
            @Schema(description = "Workspace-relative path to a single document file.", required = true, examples = {"docs/chapter5/sample.pdf"})
            String path,
            @Schema(description = "Optional source identifier to use for the ingested document.", examples = {"sample-pdf"})
            String sourceId
    ) {
    }

    public record RagDirectoryIngestRequest(
            @NotBlank
            @Schema(description = "Workspace-relative path to a directory containing documents.", required = true, examples = {"docs/chapter5/samples"})
            String path,
            @Schema(description = "Optional source id prefix to prepend to each ingested file.", examples = {"samples"})
            String sourceIdPrefix,
            @Schema(description = "Whether to recurse into subdirectories. Defaults to false.")
            Boolean recursive,
            @Schema(description = "Maximum number of directory entries to inspect. Defaults to 20.")
            Integer maxFiles
    ) {
    }

    public record RagIngestPathResponse(
            @Schema(description = "Source identifier used during ingest.")
            String sourceId,
            @Schema(description = "Original path submitted for ingest.")
            String path,
            @Schema(description = "Resolved workspace path for the document.")
            String resolvedPath,
            @Schema(description = "Ingest or read status for the supplied document.")
            String status,
            @Schema(description = "Underlying document-read status.")
            String documentStatus,
            @Schema(description = "Detected file type.")
            String fileType,
            @Schema(description = "Detected content type.")
            String contentType,
            @Schema(description = "Whether extracted text was truncated.")
            boolean wasTruncated,
            @Schema(description = "Original content length before normalization/truncation.")
            int originalLength,
            @Schema(description = "Length of the extracted, normalized text.")
            int extractedLength,
            @Schema(description = "Short extraction note.")
            String extractionNote,
            @Schema(description = "Trace events from document read and ingest.")
            List<String> traceEvents,
            @Schema(description = "Number of document chunks created by the ingest.")
            int chunkCount,
            @Schema(description = "Chunk summaries written to the RAG stack.")
            List<RagChunkResponse> chunks,
            @Schema(description = "Error message when ingest or extraction failed.")
            String error
    ) {
        static RagIngestPathResponse from(RagPathIngestResult result) {
            List<RagChunkResponse> chunks = result.chunks().stream()
                    .map(RagChunkResponse::fromIngest)
                    .toList();
            return new RagIngestPathResponse(
                    result.sourceId(),
                    result.path(),
                    result.resolvedPath(),
                    result.status(),
                    result.documentStatus(),
                    result.fileType(),
                    result.contentType(),
                    result.wasTruncated(),
                    result.originalLength(),
                    result.extractedLength(),
                    result.extractionNote(),
                    result.traceEvents(),
                    result.chunkCount(),
                    chunks,
                    result.error()
            );
        }
    }

    public record RagQueryResponse(
            @Schema(description = "Search text used to query the knowledge base.", example = "Which text mentions PostgreSQL?")
            String query,
            @Schema(description = "Simple answer assembled from the matching RAG chunks.", example = "Source docs/postgresql.txt mentions PostgreSQL: PostgreSQL is an open-source relational database.")
            String answer,
            @Schema(description = "The highest-ranked chunk returned by retrieval. Useful for debugging the winning source.", example = "{\"sourceId\":\"docs/postgresql.txt\",\"chunkIndex\":0,\"text\":\"PostgreSQL is an open-source relational database.\",\"similarity\":0.92}")
            RagChunkResponse bestChunk,
            @Schema(description = "Compact citation summaries for the retrieved chunks.", example = "[{\"sourceId\":\"docs/postgresql.txt\",\"sourcePath\":\"docs/postgresql.txt\",\"chunkIndex\":0,\"chunkId\":\"docs/postgresql.txt:0:abcd1234\",\"fileType\":\"txt\",\"contentType\":\"text/plain\",\"documentStatus\":\"TEXT_EXTRACTED\",\"sectionHint\":\"PostgreSQL is an open-source relational database.\",\"similarity\":0.92}]")
            List<RagCitationResponse> citations,
            @Schema(description = "Matched chunk summaries ordered by relevance.", example = "[{\"sourceId\":\"docs/postgresql.txt\",\"chunkIndex\":0,\"text\":\"PostgreSQL is an open-source relational database.\",\"metadata\":{\"sourceId\":\"docs/postgresql.txt\",\"sourcePath\":\"docs/postgresql.txt\",\"fileType\":\"txt\",\"contentType\":\"text/plain\",\"documentStatus\":\"TEXT_EXTRACTED\",\"chunkId\":\"docs/postgresql.txt:0:abcd1234\"},\"similarity\":0.92}]")
            List<RagChunkResponse> chunks
    ) {
    }

    public record RagCitationResponse(
            @Schema(description = "Source document identifier.", example = "docs/postgresql.txt")
            String sourceId,
            @Schema(description = "Workspace-relative source path when available.", example = "docs/postgresql.txt")
            String sourcePath,
            @Schema(description = "Zero-based chunk index within the source document.", example = "0")
            int chunkIndex,
            @Schema(description = "Stable chunk identifier.", example = "docs/postgresql.txt:0:abcd1234")
            String chunkId,
            @Schema(description = "Detected file type.", example = "txt")
            String fileType,
            @Schema(description = "Detected content type.", example = "text/plain")
            String contentType,
            @Schema(description = "Document-read status for the source.", example = "TEXT_EXTRACTED")
            String documentStatus,
            @Schema(description = "Short hint about the chunk's opening section.", example = "PostgreSQL is an open-source relational database.")
            String sectionHint,
            @Schema(description = "Similarity score for query responses.", example = "0.92")
            Double similarity
    ) {
        static RagCitationResponse fromQuery(RetrievalResult result) {
            DocumentChunk chunk = result.chunk();
            Map<String, String> metadata = chunk.metadata();
            return new RagCitationResponse(
                    chunk.sourceId(),
                    metadata.getOrDefault("sourcePath", ""),
                    chunk.chunkIndex(),
                    metadata.getOrDefault("chunkId", ""),
                    metadata.getOrDefault("fileType", ""),
                    metadata.getOrDefault("contentType", ""),
                    metadata.getOrDefault("documentStatus", ""),
                    metadata.getOrDefault("sectionHint", ""),
                    result.similarity()
            );
        }
    }

    public record RagChunkResponse(
            @Schema(description = "Source document identifier.", example = "docs/postgresql.txt")
            String sourceId,
            @Schema(description = "Zero-based chunk index within the source document.", example = "0")
            int chunkIndex,
            @Schema(description = "Chunk text returned by the RAG flow.", example = "PostgreSQL is an open-source relational database.")
            String text,
            @Schema(description = "Chunk metadata returned by ingest/query flows.")
            Map<String, String> metadata,
            @Schema(description = "Similarity score for query responses. Null for ingest responses.", example = "0.92")
            Double similarity
    ) {
        static RagChunkResponse fromIngest(DocumentChunk chunk) {
            return new RagChunkResponse(chunk.sourceId(), chunk.chunkIndex(), chunk.text(), chunk.metadata(), null);
        }

        static RagChunkResponse fromQuery(RetrievalResult result) {
            DocumentChunk chunk = result.chunk();
            return new RagChunkResponse(chunk.sourceId(), chunk.chunkIndex(), chunk.text(), chunk.metadata(), result.similarity());
        }
    }
}
