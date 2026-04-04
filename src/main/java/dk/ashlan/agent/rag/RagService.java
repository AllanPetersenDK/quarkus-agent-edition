package dk.ashlan.agent.rag;

import dk.ashlan.agent.document.DocumentReadResult;
import dk.ashlan.agent.document.DocumentReadService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.LinkOption;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

@ApplicationScoped
public class RagService {
    private static final int DEFAULT_DIRECTORY_LIMIT = 20;
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

    public List<DocumentChunk> ingest(String sourceId, String text, Map<String, String> documentMetadata) {
        return ingestionService.ingest(sourceId, text, documentMetadata);
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
            List<DocumentChunk> chunks = ingestionService.ingest(effectiveSourceId, readResult.extractedText(), documentMetadata(effectiveSourceId, path, readResult));
            return RagPathIngestResult.fromSuccess(effectiveSourceId, path, readResult, chunks);
        } catch (RuntimeException exception) {
            String effectiveSourceId = sourceId == null || sourceId.isBlank() ? deriveSourceId(null, path) : sourceId.trim();
            return RagPathIngestResult.fromException(effectiveSourceId, path, exception.getMessage());
        }
    }

    public RagDirectoryIngestResult ingestDirectory(String path, String sourceIdPrefix, boolean recursive, int maxFiles) {
        if (documentReadService == null) {
            throw new IllegalStateException("Directory ingest requires a document read service");
        }
        int limit = maxFiles > 0 ? maxFiles : DEFAULT_DIRECTORY_LIMIT;
        try {
            Path resolvedDirectory = documentReadService.resolvePath(path);
            if (!Files.exists(resolvedDirectory, LinkOption.NOFOLLOW_LINKS)) {
                return new RagDirectoryIngestResult(path, "", recursive, limit, 0, 0, 0, 1, List.of(), "directory does not exist: " + path);
            }
            if (!Files.isDirectory(resolvedDirectory, LinkOption.NOFOLLOW_LINKS)) {
                return new RagDirectoryIngestResult(path, resolvedDirectory.toString(), recursive, limit, 0, 0, 0, 1, List.of(), "directory ingest requires a directory path: " + path);
            }
            List<Path> candidates = collectDirectoryCandidates(resolvedDirectory, recursive, limit);
            List<RagDirectoryIngestFileResult> results = new ArrayList<>();
            int ingested = 0;
            int skipped = 0;
            int failed = 0;
            for (Path candidate : candidates) {
                RagDirectoryIngestFileResult result = ingestDirectoryCandidate(resolvedDirectory, candidate, sourceIdPrefix);
                results.add(result);
                switch (result.status()) {
                    case "INGESTED" -> ingested++;
                    case "SKIPPED_UNSUPPORTED", "SKIPPED_DIRECTORY" -> skipped++;
                    default -> failed++;
                }
            }
            return new RagDirectoryIngestResult(
                    path,
                    resolvedDirectory.toString(),
                    recursive,
                    limit,
                    candidates.size(),
                    ingested,
                    skipped,
                    failed,
                    results,
                    ""
            );
        } catch (RuntimeException exception) {
            return new RagDirectoryIngestResult(path, "", recursive, limit, 0, 0, 0, 1, List.of(), exception.getMessage());
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
        if (resolvedPath != null && documentReadService != null) {
            try {
                Path root = documentReadService.workspaceRoot();
                Path normalized = resolvedPath.toAbsolutePath().normalize();
                if (normalized.startsWith(root.toAbsolutePath().normalize())) {
                    return root.toAbsolutePath().normalize().relativize(normalized).toString().replace('\\', '/');
                }
            } catch (RuntimeException ignored) {
                // fall through to legacy behavior
            }
        }
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

    private List<Path> collectDirectoryCandidates(Path root, boolean recursive, int maxFiles) {
        try (Stream<Path> stream = recursive ? Files.walk(root) : Files.list(root)) {
            return stream
                    .filter(path -> !path.equals(root))
                    .sorted(Comparator.comparing(path -> root.relativize(path).toString()))
                    .limit(maxFiles)
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to enumerate directory: " + root, exception);
        }
    }

    private RagDirectoryIngestFileResult ingestDirectoryCandidate(Path root, Path candidate, String sourceIdPrefix) {
        String relativePath = workspaceRelativePath(candidate);
        String sourceId = buildSourceId(sourceIdPrefix, relativePath);
        try {
            if (Files.isDirectory(candidate, LinkOption.NOFOLLOW_LINKS)) {
                return new RagDirectoryIngestFileResult(relativePath, candidate.toString(), sourceId, "SKIPPED_DIRECTORY", "DIRECTORY_UNSUPPORTED", "", 0, List.of("directory:skipped"), "directory entries are not ingested");
            }
            DocumentReadResult readResult = documentReadService.readDocumentFile(candidate, relativePath);
            if (!readResult.success()) {
                return new RagDirectoryIngestFileResult(
                        relativePath,
                        candidate.toString(),
                        sourceId,
                        mapIngestStatus(readResult.status()),
                        readResult.status(),
                        readResult.fileType(),
                        0,
                        readResult.traceEvents(),
                        readResult.extractionNote()
                );
            }
            List<DocumentChunk> chunks = ingestionService.ingest(sourceId, readResult.extractedText(), documentMetadata(sourceId, relativePath, readResult));
            return new RagDirectoryIngestFileResult(
                    relativePath,
                    candidate.toString(),
                    sourceId,
                    "INGESTED",
                    readResult.status(),
                    readResult.fileType(),
                    chunks.size(),
                    readResult.traceEvents(),
                    ""
            );
        } catch (RuntimeException exception) {
            String status = classifyDirectoryFailure(exception.getMessage());
            return new RagDirectoryIngestFileResult(relativePath, candidate.toString(), sourceId, status, status, "", 0, List.of("document:directory-ingest-failed"), exception.getMessage());
        }
    }

    private String buildSourceId(String sourceIdPrefix, String relativePath) {
        String normalizedPath = relativePath == null ? "" : relativePath.replace('\\', '/');
        if (sourceIdPrefix == null || sourceIdPrefix.isBlank()) {
            return normalizedPath;
        }
        String prefix = sourceIdPrefix.trim().replace('\\', '/');
        if (normalizedPath.isBlank()) {
            return prefix;
        }
        return prefix + "/" + normalizedPath;
    }

    private String workspaceRelativePath(Path candidate) {
        if (candidate == null || documentReadService == null) {
            return "";
        }
        Path root = documentReadService.workspaceRoot().toAbsolutePath().normalize();
        Path normalized = candidate.toAbsolutePath().normalize();
        if (!normalized.startsWith(root)) {
            return normalized.toString().replace('\\', '/');
        }
        return root.relativize(normalized).toString().replace('\\', '/');
    }

    private Map<String, String> documentMetadata(String sourceId, String originalPath, DocumentReadResult readResult) {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("sourceId", sourceId);
        metadata.put("source", sourceId);
        metadata.put("sourcePath", originalPath == null ? "" : originalPath.replace('\\', '/'));
        metadata.put("documentName", readResult.resolvedPath() == null || readResult.resolvedPath().getFileName() == null ? sourceId : readResult.resolvedPath().getFileName().toString());
        metadata.put("fileType", readResult.fileType());
        metadata.put("contentType", readResult.contentType());
        metadata.put("documentStatus", readResult.status());
        metadata.put("wasTruncated", String.valueOf(readResult.wasTruncated()));
        metadata.put("originalLength", String.valueOf(readResult.originalLength()));
        metadata.put("extractedLength", String.valueOf(readResult.extractedLength()));
        return metadata;
    }

    private String mapIngestStatus(String documentStatus) {
        if (documentStatus == null || documentStatus.isBlank()) {
            return "READ_FAILED";
        }
        return switch (documentStatus) {
            case "UNSUPPORTED_TYPE" -> "SKIPPED_UNSUPPORTED";
            case "DIRECTORY_UNSUPPORTED" -> "SKIPPED_DIRECTORY";
            case "SECURITY_VIOLATION", "INVALID_PATH", "RESOLUTION_FAILED" -> documentStatus;
            case "MISSING", "ERROR" -> "READ_FAILED";
            default -> "READ_FAILED";
        };
    }

    private String classifyDirectoryFailure(String message) {
        String value = message == null ? "" : message.toLowerCase();
        if (value.contains("security") || value.contains("symlink") || value.contains("traversal")) {
            return "SECURITY_VIOLATION";
        }
        if (value.contains("invalid path")) {
            return "INVALID_PATH";
        }
        if (value.contains("resolution")) {
            return "RESOLUTION_FAILED";
        }
        return "READ_FAILED";
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
