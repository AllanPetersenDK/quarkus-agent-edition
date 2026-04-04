package dk.ashlan.agent.eval.gaia;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static dk.ashlan.agent.document.DocumentTypeSupport.isAudioLike;
import static dk.ashlan.agent.document.DocumentTypeSupport.isReadableDocument;

@ApplicationScoped
public class GaiaAttachmentResolver {
    private final GaiaAudioTranscriptionService audioTranscriptionService;
    private final GaiaAttachmentExtractionService attachmentExtractionService;

    @Inject
    public GaiaAttachmentResolver(GaiaAudioTranscriptionService audioTranscriptionService, GaiaAttachmentExtractionService attachmentExtractionService) {
        this.audioTranscriptionService = audioTranscriptionService;
        this.attachmentExtractionService = attachmentExtractionService;
    }

    public GaiaAttachmentResolver(GaiaAudioTranscriptionService audioTranscriptionService) {
        this(audioTranscriptionService, new GaiaAttachmentExtractionService());
    }

    public GaiaAttachment resolve(GaiaExample example, String baseSource) {
        String filePath = example.filePath();
        String fileName = example.fileName();
        String cleanedPath = filePath == null ? "" : filePath.trim();
        if (cleanedPath.isBlank() || cleanedPath.equalsIgnoreCase("null")) {
            return new GaiaAttachment(fileName, filePath, "", GaiaAttachmentStatus.MISSING, "attachment missing", List.of("attachment:missing"));
        }

        Path localBase = resolveLocalBase(baseSource);
        if (localBase != null) {
            Path resolved = localBase.resolve(cleanedPath).normalize();
            if (!Files.exists(resolved)) {
                return new GaiaAttachment(fileName, filePath, resolved.toString(), GaiaAttachmentStatus.MISSING, "attachment file is missing: " + resolved, List.of("attachment:missing"));
            }
            String extension = dk.ashlan.agent.document.DocumentTypeSupport.extension(resolved);
            if (isReadableDocument(extension)) {
                GaiaExtractedAttachment extracted = attachmentExtractionService.extract(resolved);
                return buildExtractedAttachment(fileName, filePath, resolved, extracted);
            }
            if (isAudioLike(extension)) {
                try {
                    String transcript = audioTranscriptionService.transcribe(resolved);
                    return new GaiaAttachment(
                            fileName,
                            filePath,
                            resolved.toString(),
                            GaiaAttachmentStatus.AUDIO_TRANSCRIBED,
                            buildAudioNote(resolved, fileName, transcript),
                            List.of("attachment:present:" + safeName(fileName, resolved), "attachment:audio-transcribed")
                    );
                } catch (Exception exception) {
                    return new GaiaAttachment(
                            fileName,
                            filePath,
                            resolved.toString(),
                            GaiaAttachmentStatus.AUDIO_TRANSCRIPTION_FAILED,
                            "attachment present but audio transcription failed: " + exception.getMessage(),
                            List.of("attachment:present:" + safeName(fileName, resolved), "attachment:audio-transcription-failed")
                    );
                }
            }
            return new GaiaAttachment(
                    fileName,
                    filePath,
                    resolved.toString(),
                    GaiaAttachmentStatus.UNSUPPORTED_TYPE,
                    "attachment present but unsupported type: " + extension,
                    List.of("attachment:present:" + safeName(fileName, resolved), "attachment:unsupported-type")
            );
        }

        String resolved = resolveRemote(baseSource, cleanedPath);
        String extension = dk.ashlan.agent.document.DocumentTypeSupport.extension(cleanedPath);
        if (isReadableDocument(extension)) {
            return new GaiaAttachment(
                    fileName,
                    filePath,
                    resolved,
                    GaiaAttachmentStatus.PRESENT,
                    "GAIA attachment present at " + resolved + " but text extraction is unavailable for remote sources.",
                    List.of("attachment:present:" + safeName(fileName, Path.of(cleanedPath)), "attachment:text-extraction-unavailable")
            );
        }
        if (isAudioLike(extension)) {
            return new GaiaAttachment(
                    fileName,
                    filePath,
                    resolved,
                    GaiaAttachmentStatus.AUDIO_TRANSCRIPTION_FAILED,
                    "attachment present but audio transcription is unavailable for remote sources: " + resolved,
                    List.of("attachment:present:" + safeName(fileName, Path.of(cleanedPath)), "attachment:audio-transcription-failed")
            );
        }
        return new GaiaAttachment(
                fileName,
                filePath,
                resolved,
                GaiaAttachmentStatus.UNSUPPORTED_TYPE,
                "attachment present but unsupported type: " + extension,
                List.of("attachment:present:" + safeName(fileName, Path.of(cleanedPath)), "attachment:unsupported-type")
        );
    }

    public String toContextNote(GaiaAttachment attachment) {
        if (attachment == null) {
            return "";
        }
        return switch (attachment.status()) {
            case MISSING -> "GAIA attachment missing: " + defaultText(attachment.fileName(), attachment.filePath());
            case UNSUPPORTED_TYPE -> "GAIA attachment present but unsupported: " + attachment.note() + ". Resolved path: " + attachment.resolvedPath();
            case AUDIO_TRANSCRIPTION_FAILED -> "GAIA audio attachment present but transcription failed: " + attachment.note() + ". Resolved path: " + attachment.resolvedPath();
            case TEXT_EXTRACTION_FAILED -> "GAIA attachment text extraction failed: " + attachment.note() + ". Resolved path: " + attachment.resolvedPath();
            case TEXT_EXTRACTED -> "GAIA attachment text extracted from " + defaultText(attachment.fileName(), attachment.filePath()) + ". Use this content when answering the task.\n" + attachment.note();
            case PRESENT, AUDIO_TRANSCRIBED -> "GAIA attachment available: " + attachment.note();
        };
    }

    private String buildAudioNote(Path path, String fileName, String transcript) {
        String preview = transcript == null ? "" : transcript;
        if (preview.length() > 4000) {
            preview = preview.substring(0, 4000) + "...";
        }
        return "GAIA audio transcript for " + defaultText(fileName, path.getFileName().toString()) + ":\n" + preview;
    }

    private Path resolveLocalBase(String baseSource) {
        if (baseSource == null || baseSource.isBlank()) {
            return null;
        }
        try {
            if (baseSource.startsWith("file:")) {
                Path path = Path.of(URI.create(baseSource));
                return Files.isDirectory(path) ? path : path.getParent();
            }
            if (baseSource.contains("://")) {
                return null;
            }
            Path path = Path.of(baseSource);
            return Files.isDirectory(path) ? path : path.getParent();
        } catch (Exception exception) {
            return null;
        }
    }

    private String resolveRemote(String baseSource, String cleanedPath) {
        try {
            URI base = URI.create(baseSource.endsWith("/") ? baseSource : baseSource + "/");
            return base.resolve(cleanedPath).toString();
        } catch (Exception exception) {
            return cleanedPath;
        }
    }

    private GaiaAttachment buildExtractedAttachment(String fileName, String filePath, Path resolved, GaiaExtractedAttachment extracted) {
        List<String> traceEvents = new java.util.ArrayList<>();
        traceEvents.add("attachment:present:" + safeName(fileName, resolved));
        traceEvents.addAll(extracted.traceEvents());
        String note = extracted.extractionNote();
        if (extracted.hasText() && extracted.extractedText() != null && !extracted.extractedText().isBlank()) {
            note = note + "\n" + extracted.extractedText();
        }
        return new GaiaAttachment(
                fileName,
                filePath,
                resolved.toString(),
                extracted.status(),
                note,
                List.copyOf(traceEvents)
        );
    }

    private String safeName(String fileName, Path fallback) {
        if (fileName != null && !fileName.isBlank()) {
            return fileName;
        }
        return fallback.getFileName().toString();
    }

    private String defaultText(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second == null ? "" : second;
    }
}
