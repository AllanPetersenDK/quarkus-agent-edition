package dk.ashlan.agent.eval.gaia;

import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@ApplicationScoped
public class GaiaAttachmentResolver {
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
            String extension = extension(resolved.getFileName().toString());
            if (isTextLike(extension)) {
                return new GaiaAttachment(
                        fileName,
                        filePath,
                        resolved.toString(),
                        GaiaAttachmentStatus.PRESENT,
                        buildTextNote(resolved, fileName),
                        List.of("attachment:present:" + safeName(fileName, resolved), "attachment:text-readable")
                );
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
        String extension = extension(cleanedPath);
        if (isTextLike(extension)) {
            return new GaiaAttachment(
                    fileName,
                    filePath,
                    resolved,
                    GaiaAttachmentStatus.PRESENT,
                    "attachment present at " + resolved,
                    List.of("attachment:present:" + safeName(fileName, Path.of(cleanedPath)))
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
            case PRESENT -> "GAIA attachment available: " + attachment.note();
        };
    }

    private String buildTextNote(Path path, String fileName) {
        try {
            String text = Files.readString(path, StandardCharsets.UTF_8);
            String preview = text.length() > 4000 ? text.substring(0, 4000) + "..." : text;
            return "GAIA attachment text preview for " + defaultText(fileName, path.getFileName().toString()) + ":\n" + preview;
        } catch (IOException exception) {
            return "GAIA attachment present but could not be read as text: " + path;
        }
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

    private boolean isTextLike(String extension) {
        return switch (extension.toLowerCase()) {
            case "txt", "md", "csv", "json", "jsonl", "ndjson", "yaml", "yml" -> true;
            default -> false;
        };
    }

    private String extension(String value) {
        int index = value.lastIndexOf('.');
        if (index < 0 || index == value.length() - 1) {
            return "";
        }
        return value.substring(index + 1);
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
