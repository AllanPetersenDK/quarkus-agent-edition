package dk.ashlan.agent.document;

import dk.ashlan.agent.code.WorkspaceService;
import dk.ashlan.agent.eval.gaia.GaiaAttachmentExtractionService;
import dk.ashlan.agent.eval.gaia.GaiaAttachmentStatus;
import dk.ashlan.agent.eval.gaia.GaiaAudioTranscriptionService;
import dk.ashlan.agent.eval.gaia.GaiaExtractedAttachment;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class DocumentReadService {
    private static final int MAX_AUDIO_CHARS = 4000;

    private final WorkspaceService workspaceService;
    private final GaiaAttachmentExtractionService attachmentExtractionService;
    private final GaiaAudioTranscriptionService audioTranscriptionService;

    @Inject
    public DocumentReadService(
            WorkspaceService workspaceService,
            GaiaAttachmentExtractionService attachmentExtractionService,
            GaiaAudioTranscriptionService audioTranscriptionService
    ) {
        this.workspaceService = workspaceService;
        this.attachmentExtractionService = attachmentExtractionService;
        this.audioTranscriptionService = audioTranscriptionService;
    }

    public DocumentReadResult readTextFile(Path resolvedPath, String originalPath) {
        return readDocumentInternal(resolvedPath, originalPath, true);
    }

    public DocumentReadResult readTextFile(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return new DocumentReadResult("INVALID_PATH", null, "", "application/octet-stream", "", "path is required", List.of("document:invalid-path"), false, false, 0, 0);
        }
        return readFromRawPath(rawPath, true);
    }

    public DocumentReadResult readDocumentFile(Path resolvedPath, String originalPath) {
        return readDocumentInternal(resolvedPath, originalPath, false);
    }

    public DocumentReadResult readDocumentFile(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return new DocumentReadResult("INVALID_PATH", null, "", "application/octet-stream", "", "path is required", List.of("document:invalid-path"), false, false, 0, 0);
        }
        return readFromRawPath(rawPath, false);
    }

    private DocumentReadResult readFromRawPath(String rawPath, boolean textOnly) {
        try {
            return readDocumentInternal(workspaceService.resolve(rawPath), rawPath, textOnly);
        } catch (IllegalArgumentException exception) {
            String status = classifyPathError(exception.getMessage());
            return failure(status, null, rawPath, exception.getMessage(), List.of("document:path-rejected"));
        } catch (IllegalStateException exception) {
            return failure("RESOLUTION_FAILED", null, rawPath, exception.getMessage(), List.of("document:read-failed"));
        }
    }

    private DocumentReadResult readDocumentInternal(Path resolvedPath, String originalPath, boolean textOnly) {
        try {
            Path safePath = validateResolvedPath(resolvedPath, originalPath);
            if (!Files.exists(safePath, LinkOption.NOFOLLOW_LINKS)) {
                return failure("MISSING", safePath, originalPath, "document does not exist: " + safePath, List.of("document:missing"));
            }
            if (Files.isDirectory(safePath, LinkOption.NOFOLLOW_LINKS)) {
                return failure("DIRECTORY_UNSUPPORTED", safePath, originalPath, "directory ingest/read is not supported yet: " + safePath, List.of("document:directory-not-supported"));
            }

            String extension = DocumentTypeSupport.extension(safePath);
            if (textOnly && !DocumentTypeSupport.isTextLike(extension)) {
                return failure("UNSUPPORTED_TYPE", safePath, originalPath, "unsupported text type: " + extension, List.of("document:unsupported-type"));
            }

            if (DocumentTypeSupport.isTextLike(extension) || "pdf".equals(extension)) {
                GaiaExtractedAttachment extracted = attachmentExtractionService.extract(safePath);
                return mapExtracted(safePath, originalPath, extracted);
            }
            if (!textOnly && DocumentTypeSupport.isAudioLike(extension)) {
                return transcribeAudio(safePath, originalPath);
            }

            return failure("UNSUPPORTED_TYPE", safePath, originalPath, "unsupported document type: " + extension, List.of("document:unsupported-type"));
        } catch (RuntimeException exception) {
            return failure("ERROR", resolvedPath == null ? null : resolvedPath.toAbsolutePath().normalize(), originalPath, "document read failed: " + exception.getMessage(), List.of("document:read-failed"));
        }
    }

    private DocumentReadResult mapExtracted(Path resolvedPath, String originalPath, GaiaExtractedAttachment extracted) {
        boolean success = extracted.status() == GaiaAttachmentStatus.TEXT_EXTRACTED;
        List<String> traceEvents = new ArrayList<>(extracted.traceEvents());
        traceEvents.add(0, "document:resolved:" + resolvedPath.getFileName());
        return new DocumentReadResult(
                extracted.status().name(),
                resolvedPath.toAbsolutePath().normalize(),
                extracted.fileType(),
                extracted.contentType(),
                extracted.extractedText(),
                extracted.extractionNote(),
                List.copyOf(traceEvents),
                success,
                extracted.wasTruncated(),
                extracted.originalLength(),
                extracted.extractedLength()
        );
    }

    private DocumentReadResult transcribeAudio(Path resolvedPath, String originalPath) {
        try {
            String transcript = audioTranscriptionService.transcribe(resolvedPath);
            String normalized = transcript == null ? "" : transcript.replaceAll("\\s+", " ").trim();
            if (normalized.isBlank()) {
                return failure("AUDIO_TRANSCRIPTION_FAILED", resolvedPath, originalPath, "audio transcription produced no text", List.of("attachment:audio-transcription-failed"));
            }
            boolean wasTruncated = false;
            if (normalized.length() > MAX_AUDIO_CHARS) {
                normalized = normalized.substring(0, MAX_AUDIO_CHARS);
                wasTruncated = true;
            }
            List<String> traceEvents = new ArrayList<>();
            traceEvents.add("document:resolved:" + resolvedPath.getFileName());
            traceEvents.add("attachment:audio-transcribed");
            if (wasTruncated) {
                traceEvents.add("attachment:text-truncated");
            }
            String note = "GAIA audio transcript for " + resolvedPath.getFileName() + ".";
            if (wasTruncated) {
                note = note + " Transcript was truncated to " + MAX_AUDIO_CHARS + " characters.";
            }
            return new DocumentReadResult(
                    "AUDIO_TRANSCRIBED",
                    resolvedPath.toAbsolutePath().normalize(),
                    DocumentTypeSupport.extension(resolvedPath),
                    "text/plain",
                    normalized,
                    note,
                    List.copyOf(traceEvents),
                    true,
                    wasTruncated,
                    transcript == null ? 0 : transcript.length(),
                    normalized.length()
            );
        } catch (RuntimeException exception) {
            return failure("AUDIO_TRANSCRIPTION_FAILED", resolvedPath, originalPath, "audio transcription failed: " + exception.getMessage(), List.of("attachment:audio-transcription-failed"));
        }
    }

    private DocumentReadResult failure(String status, Path resolvedPath, String originalPath, String message, List<String> traceEvents) {
        Path safeResolved = resolvedPath == null ? null : resolvedPath.toAbsolutePath().normalize();
        return new DocumentReadResult(
                status,
                safeResolved,
                safeResolved == null ? "" : DocumentTypeSupport.extension(safeResolved),
                "application/octet-stream",
                "",
                message,
                List.copyOf(traceEvents),
                false,
                false,
                0,
                0
        );
    }

    private String classifyPathError(String message) {
        String value = message == null ? "" : message.toLowerCase();
        if (value.contains("path traversal") || value.contains("symlink access")) {
            return "SECURITY_VIOLATION";
        }
        if (value.contains("invalid path")) {
            return "INVALID_PATH";
        }
        return "RESOLUTION_FAILED";
    }

    private Path validateResolvedPath(Path resolvedPath, String originalPath) {
        if (resolvedPath == null) {
            throw new IllegalArgumentException("path is required");
        }
        Path safe = resolvedPath.toAbsolutePath().normalize();
        Path root = workspaceService.root().toAbsolutePath().normalize();
        if (!safe.startsWith(root)) {
            throw new IllegalArgumentException("resolved path escapes workspace root: " + defaultText(originalPath, safe.toString()));
        }
        Path cursor = root;
        for (Path part : root.relativize(safe)) {
            cursor = cursor.resolve(part);
            if (Files.exists(cursor, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(cursor)) {
                throw new IllegalArgumentException("Symlink access is not allowed: " + defaultText(originalPath, safe.toString()));
            }
        }
        return safe;
    }

    private String defaultText(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second == null ? "" : second;
    }
}
