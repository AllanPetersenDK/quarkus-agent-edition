package dk.ashlan.agent.rag;

import dk.ashlan.agent.document.DocumentReadResult;
import dk.ashlan.agent.document.DocumentReadService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.nio.file.Path;
import java.util.List;

@ApplicationScoped
public class RagService {
    private final DocumentIngestionService ingestionService;
    private final Retriever retriever;
    private final RagAnswerBuilder answerBuilder;
    private final DocumentReadService documentReadService;

    @Inject
    public RagService(DocumentIngestionService ingestionService, Retriever retriever, RagAnswerBuilder answerBuilder, DocumentReadService documentReadService) {
        this.ingestionService = ingestionService;
        this.retriever = retriever;
        this.answerBuilder = answerBuilder;
        this.documentReadService = documentReadService;
    }

    public RagService(DocumentIngestionService ingestionService, Retriever retriever) {
        this(ingestionService, retriever, new RagAnswerBuilder(), null);
    }

    public RagService(DocumentIngestionService ingestionService, Retriever retriever, RagAnswerBuilder answerBuilder) {
        this(ingestionService, retriever, answerBuilder, null);
    }

    public RagService(DocumentIngestionService ingestionService, Retriever retriever, DocumentReadService documentReadService) {
        this(ingestionService, retriever, new RagAnswerBuilder(), documentReadService);
    }

    public List<DocumentChunk> ingest(String sourceId, String text) {
        return ingestionService.ingest(sourceId, text);
    }

    public RagPathIngestResult ingestPath(String path, String sourceId) {
        if (documentReadService == null) {
            throw new IllegalStateException("Path ingest requires a document read service");
        }
        try {
            DocumentReadResult readResult = documentReadService.readDocumentFile(path);
            String effectiveSourceId = sourceId == null || sourceId.isBlank() ? deriveSourceId(readResult.resolvedPath(), path) : sourceId.trim();
            if (!readResult.success()) {
                return RagPathIngestResult.fromReadFailure(effectiveSourceId, path, readResult);
            }
            List<DocumentChunk> chunks = ingestionService.ingest(effectiveSourceId, readResult.extractedText());
            return RagPathIngestResult.fromSuccess(effectiveSourceId, path, readResult, chunks);
        } catch (RuntimeException exception) {
            String effectiveSourceId = sourceId == null || sourceId.isBlank() ? deriveSourceId(null, path) : sourceId.trim();
            return RagPathIngestResult.fromException(effectiveSourceId, path, exception.getMessage());
        }
    }

    public List<RetrievalResult> retrieve(String query, int topK) {
        return retriever.retrieve(query, topK);
    }

    public String answer(String query, int topK) {
        return answer(query, retrieve(query, topK));
    }

    public String answer(String query, List<RetrievalResult> results) {
        return answerBuilder.build(query, results);
    }

    private String deriveSourceId(Path resolvedPath, String fallbackPath) {
        if (fallbackPath != null && !fallbackPath.isBlank()) {
            return fallbackPath.trim().replace('\\', '/');
        }
        if (resolvedPath != null) {
            Path fileName = resolvedPath.getFileName();
            if (fileName != null) {
                return fileName.toString();
            }
        }
        return "document";
    }

    public record RagPathIngestResult(
            String sourceId,
            String path,
            String resolvedPath,
            String status,
            String documentStatus,
            String fileType,
            String contentType,
            boolean wasTruncated,
            int originalLength,
            int extractedLength,
            String extractionNote,
            List<String> traceEvents,
            int chunkCount,
            List<DocumentChunk> chunks,
            String error
    ) {
        static RagPathIngestResult fromSuccess(String sourceId, String path, DocumentReadResult readResult, List<DocumentChunk> chunks) {
            return new RagPathIngestResult(
                    sourceId,
                    path,
                    readResult.resolvedPath() == null ? "" : readResult.resolvedPath().toString(),
                    "INGESTED",
                    readResult.status(),
                    readResult.fileType(),
                    readResult.contentType(),
                    readResult.wasTruncated(),
                    readResult.originalLength(),
                    readResult.extractedLength(),
                    readResult.extractionNote(),
                    readResult.traceEvents(),
                    chunks.size(),
                    chunks,
                    ""
            );
        }

        static RagPathIngestResult fromReadFailure(String sourceId, String path, DocumentReadResult readResult) {
            return new RagPathIngestResult(
                    sourceId,
                    path,
                    readResult.resolvedPath() == null ? "" : readResult.resolvedPath().toString(),
                    readResult.status(),
                    readResult.status(),
                    readResult.fileType(),
                    readResult.contentType(),
                    readResult.wasTruncated(),
                    readResult.originalLength(),
                    readResult.extractedLength(),
                    readResult.extractionNote(),
                    readResult.traceEvents(),
                    0,
                    List.of(),
                    readResult.extractionNote()
            );
        }

        static RagPathIngestResult fromException(String sourceId, String path, String message) {
            return new RagPathIngestResult(
                    sourceId,
                    path,
                    "",
                    "ERROR",
                    "ERROR",
                    "",
                    "",
                    false,
                    0,
                    0,
                    message,
                    List.of("document:ingest-failed"),
                    0,
                    List.of(),
                    message
            );
        }
    }
}
