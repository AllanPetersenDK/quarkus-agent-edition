package dk.ashlan.agent.tools.filesystem;

import dk.ashlan.agent.code.WorkspaceService;
import dk.ashlan.agent.document.DocumentReadResult;
import dk.ashlan.agent.document.DocumentReadService;
import dk.ashlan.agent.eval.gaia.GaiaAttachmentExtractionService;
import dk.ashlan.agent.eval.gaia.GaiaAudioTranscriptionService;
import dk.ashlan.agent.tools.JsonToolResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@ApplicationScoped
public class FilesystemToolService {
    private static final int DEFAULT_MAX_LIST_ENTRIES = 50;
    private static final int DEFAULT_MAX_FILE_NAMES = 12;

    private final Path workspaceRoot;
    private final DocumentReadService documentReadService;

    @Inject
    public FilesystemToolService(
            WorkspaceService workspaceService,
            DocumentReadService documentReadService
    ) {
        this.workspaceRoot = initializeRoot(Objects.requireNonNull(workspaceService, "workspaceService").root());
        this.documentReadService = Objects.requireNonNull(documentReadService, "documentReadService");
    }

    public FilesystemToolService(
            Path workspaceRoot,
            GaiaAttachmentExtractionService attachmentExtractionService,
            GaiaAudioTranscriptionService audioTranscriptionService
    ) {
        this.workspaceRoot = initializeRoot(Objects.requireNonNull(workspaceRoot, "workspaceRoot"));
        this.documentReadService = new DocumentReadService(
                new WorkspaceService(this.workspaceRoot.toString()),
                Objects.requireNonNull(attachmentExtractionService, "attachmentExtractionService"),
                Objects.requireNonNull(audioTranscriptionService, "audioTranscriptionService")
        );
    }

    public Path workspaceRoot() {
        return workspaceRoot;
    }

    public JsonToolResult unzipFile(String zipPath, String extractTo) {
        try {
            Path zip = resolveRequired(zipPath);
            if (!Files.isRegularFile(zip)) {
                return failure("unzip_file", "zip path does not exist or is not a file: " + zip, Map.of(
                        "status", "error",
                        "zipPath", zipPath,
                        "resolvedZipPath", zip.toString()
                ));
            }
            Path destination = extractTo == null || extractTo.isBlank()
                    ? defaultExtractDir(zip)
                    : resolveDirectory(extractTo);
            Files.createDirectories(destination);

            List<Map<String, Object>> extracted = new ArrayList<>();
            int fileCount = 0;
            try (InputStream input = Files.newInputStream(zip);
                 ZipInputStream zipInputStream = new ZipInputStream(input, StandardCharsets.UTF_8)) {
                ZipEntry entry;
                while ((entry = zipInputStream.getNextEntry()) != null) {
                    Path resolvedEntry = resolveZipEntry(destination, entry.getName());
                    if (entry.isDirectory()) {
                        Files.createDirectories(resolvedEntry);
                        extracted.add(entryData(entry.getName(), "folder", null));
                        continue;
                    }
                    Files.createDirectories(resolvedEntry.getParent());
                    Files.copy(zipInputStream, resolvedEntry, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    fileCount++;
                    extracted.add(entryData(entry.getName(), "file", sizeOf(resolvedEntry)));
                }
            }
            List<Map<String, Object>> preview = extracted.size() > DEFAULT_MAX_FILE_NAMES
                    ? extracted.subList(0, DEFAULT_MAX_FILE_NAMES)
                    : extracted;
            String output = "status=ok\nzipPath=" + zip + "\ndestinationPath=" + destination + "\nextractedFiles=" + fileCount + "\nentries:\n" + formatEntries(preview, extracted.size() > preview.size());
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("status", "ok");
            data.put("zipPath", zip.toString());
            data.put("destinationPath", destination.toString());
            data.put("extractedFiles", fileCount);
            data.put("entries", extracted);
            return new JsonToolResult("unzip_file", true, output, data);
        } catch (RuntimeException | IOException exception) {
            return failure("unzip_file", "zip extraction failed: " + exception.getMessage(), Map.of(
                    "status", "error",
                    "zipPath", zipPath,
                    "extractTo", extractTo == null ? "" : extractTo
            ));
        }
    }

    public JsonToolResult listFiles(String path, boolean recursive, int maxEntries) {
        try {
            Path target = resolveOptional(path);
            if (!Files.exists(target)) {
                return failure("list_files", "path does not exist: " + target, Map.of(
                        "status", "error",
                        "path", path == null ? "" : path,
                        "resolvedPath", target.toString()
                ));
            }
            int limit = Math.max(1, maxEntries);
            List<Path> paths = collectPaths(target, recursive, limit + 1);
            boolean truncated = paths.size() > limit;
            List<Path> visiblePaths = truncated ? paths.subList(0, limit) : paths;
            List<Map<String, Object>> entries = new ArrayList<>();
            for (Path item : visiblePaths) {
                if (Files.isSymbolicLink(item)) {
                    throw new IllegalArgumentException("Symlink access is not allowed: " + relativeToRoot(item));
                }
                entries.add(entryData(relativeToRoot(item), Files.isDirectory(item) ? "folder" : "file", Files.isRegularFile(item) ? sizeOf(item) : null));
            }
            String output = "status=ok\nresolvedPath=" + target + "\nrecursive=" + recursive + "\nentries=" + entries.size() + "\npaths:\n" + formatEntries(entries, truncated);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("status", "ok");
            data.put("path", path == null ? "" : path);
            data.put("resolvedPath", target.toString());
            data.put("recursive", recursive);
            data.put("maxEntries", limit);
            data.put("truncated", truncated);
            data.put("entries", entries);
            return new JsonToolResult("list_files", true, output, data);
        } catch (RuntimeException | IOException exception) {
            return failure("list_files", "listing failed: " + exception.getMessage(), Map.of(
                    "status", "error",
                    "path", path == null ? "" : path,
                    "recursive", recursive
            ));
        }
    }

    public JsonToolResult readFile(String path) {
        try {
            Path resolved = resolveRequired(path);
            DocumentReadResult readResult = documentReadService.readTextFile(resolved, path);
            return toJsonToolResult("read_file", path, readResult, true);
        } catch (RuntimeException exception) {
            return failure("read_file", "file read failed: " + exception.getMessage(), Map.of(
                    "status", "error",
                    "path", path == null ? "" : path
            ));
        }
    }

    public JsonToolResult readDocumentFile(String path) {
        return readDocumentFile("read_document_file", path);
    }

    public JsonToolResult readMediaFile(String path) {
        return readDocumentFile("read_media_file", path);
    }

    public JsonToolResult inspectPath(String path) {
        try {
            Path resolved = resolveRequired(path);
            boolean exists = Files.exists(resolved);
            String kind = !exists ? "missing" : Files.isDirectory(resolved) ? "folder" : "file";
            String extension = exists && Files.isRegularFile(resolved) ? dk.ashlan.agent.document.DocumentTypeSupport.extension(resolved) : "";
            Long size = exists && Files.isRegularFile(resolved) ? sizeOf(resolved) : null;
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("status", "ok");
            data.put("path", path);
            data.put("resolvedPath", resolved.toString());
            data.put("exists", exists);
            data.put("kind", kind);
            data.put("extension", extension);
            data.put("size", size);
            String output = "status=ok\nexists=" + exists + "\nkind=" + kind + "\nresolvedPath=" + resolved + "\nextension=" + extension + "\nsize=" + (size == null ? "" : size);
            return new JsonToolResult("inspect_path", true, output, data);
        } catch (RuntimeException exception) {
            return failure("inspect_path", "path inspection failed: " + exception.getMessage(), Map.of(
                    "status", "error",
                    "path", path == null ? "" : path
            ));
        }
    }

    private JsonToolResult readDocumentFile(String toolName, String path) {
        try {
            Path resolved = resolveRequired(path);
            DocumentReadResult readResult = documentReadService.readDocumentFile(resolved, path);
            return toJsonToolResult(toolName, path, readResult, false);
        } catch (RuntimeException exception) {
            return failure(toolName, "document read failed: " + exception.getMessage(), Map.of(
                    "status", "error",
                    "path", path == null ? "" : path
            ));
        }
    }

    private List<Path> collectPaths(Path target, boolean recursive, int maxEntries) throws IOException {
        List<Path> paths = new ArrayList<>();
        if (Files.isDirectory(target)) {
            if (recursive) {
                try (var stream = Files.walk(target)) {
                    stream.filter(path -> !path.equals(target))
                            .sorted()
                            .limit(maxEntries)
                            .forEach(paths::add);
                }
            } else {
                try (var stream = Files.list(target)) {
                    stream.sorted()
                            .limit(maxEntries)
                            .forEach(paths::add);
                }
            }
        } else {
            paths.add(target);
        }
        return paths;
    }

    private Path defaultExtractDir(Path zip) {
        String name = zip.getFileName() == null ? "archive" : stripExtension(zip.getFileName().toString());
        return workspaceRoot.resolve("unzipped").resolve(name).normalize();
    }

    private Path resolveRequired(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            throw new IllegalArgumentException("path is required");
        }
        return resolve(rawPath);
    }

    private Path resolveOptional(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return workspaceRoot;
        }
        return resolve(rawPath);
    }

    private Path resolveDirectory(String rawPath) {
        Path resolved = resolve(rawPath);
        return resolved;
    }

    private Path resolve(String rawPath) {
        Path candidate;
        try {
            if (rawPath.startsWith("file:")) {
                candidate = Path.of(java.net.URI.create(rawPath));
            } else {
                candidate = Path.of(rawPath);
            }
        } catch (Exception exception) {
            throw new IllegalArgumentException("invalid path: " + rawPath, exception);
        }
        if (!candidate.isAbsolute()) {
            candidate = workspaceRoot.resolve(candidate);
        }
        candidate = candidate.toAbsolutePath().normalize();
        if (!candidate.startsWith(workspaceRoot)) {
            throw new IllegalArgumentException("Path traversal is not allowed: " + rawPath);
        }
        rejectSymlinkHop(candidate, rawPath);
        return candidate;
    }

    private Path resolveZipEntry(Path destination, String entryName) {
        Path resolved = destination.resolve(entryName).normalize();
        if (!resolved.startsWith(destination)) {
            throw new IllegalArgumentException("Zip entry would escape destination: " + entryName);
        }
        rejectSymlinkHop(resolved, entryName);
        return resolved;
    }

    private String stripExtension(String value) {
        int index = value.lastIndexOf('.');
        if (index < 0) {
            return value;
        }
        return value.substring(0, index);
    }

    private long sizeOf(Path path) {
        try {
            return Files.size(path);
        } catch (IOException exception) {
            return -1L;
        }
    }

    private Map<String, Object> entryData(String path, String kind, Long size) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("path", path);
        entry.put("kind", kind);
        if (size != null) {
            entry.put("size", size);
        }
        return entry;
    }

    private String relativeToRoot(Path path) {
        try {
            return workspaceRoot.relativize(path.toAbsolutePath().normalize()).toString();
        } catch (Exception exception) {
            return path.toString();
        }
    }

    private String formatEntries(List<Map<String, Object>> entries, boolean truncated) {
        StringBuilder builder = new StringBuilder();
        for (Map<String, Object> entry : entries) {
            builder.append("- ").append(entry.getOrDefault("path", ""));
            if (Objects.equals(entry.get("kind"), "directory") || Objects.equals(entry.get("kind"), "folder")) {
                builder.append("/");
            }
            Object size = entry.get("size");
            if (size != null) {
                builder.append(" (").append(size).append(" bytes)");
            }
            builder.append("\n");
        }
        if (truncated) {
            builder.append("- ... truncated\n");
        }
        return builder.toString();
    }

    private JsonToolResult toJsonToolResult(String toolName, String originalPath, DocumentReadResult readResult, boolean requireTextExtraction) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", readResult.status());
        data.put("path", originalPath == null ? "" : originalPath);
        data.put("resolvedPath", readResult.resolvedPath() == null ? "" : readResult.resolvedPath().toString());
        data.put("contentType", readResult.contentType());
        data.put("fileType", readResult.fileType());
        data.put("note", readResult.extractionNote());
        data.put("traceEvents", readResult.traceEvents());
        data.put("text", readResult.extractedText());
        data.put("wasTruncated", readResult.wasTruncated());
        data.put("originalLength", readResult.originalLength());
        data.put("extractedLength", readResult.extractedLength());
        String text = readResult.extractedText() == null ? "" : readResult.extractedText();
        String output = "status=" + readResult.status().toLowerCase(Locale.ROOT)
                + "\nresolvedPath=" + readResult.resolvedPath()
                + "\ncontentType=" + readResult.contentType()
                + "\ntext:\n" + text;
        boolean success = readResult.success();
        if (requireTextExtraction) {
            success = success && "TEXT_EXTRACTED".equals(readResult.status());
        }
        if (!success && "TEXT_EXTRACTED".equals(readResult.status())) {
            success = true;
        }
        return new JsonToolResult(toolName, success, output, data);
    }

    private JsonToolResult failure(String toolName, String message, Map<String, Object> data) {
        Map<String, Object> merged = new LinkedHashMap<>(data);
        merged.putIfAbsent("status", "error");
        merged.put("error", message);
        return new JsonToolResult(toolName, false, "status=error\nmessage=" + message, merged);
    }

    private Path initializeRoot(Path root) {
        Path absoluteRoot = root.toAbsolutePath().normalize();
        try {
            if (Files.exists(absoluteRoot, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(absoluteRoot)) {
                throw new IllegalStateException("Workspace root symlink access is not allowed: " + absoluteRoot);
            }
            Files.createDirectories(absoluteRoot);
            return absoluteRoot.toRealPath();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to initialize workspace root: " + absoluteRoot, exception);
        }
    }

    private void rejectSymlinkHop(Path resolved, String rawPath) {
        Path cursor = workspaceRoot;
        for (Path part : workspaceRoot.relativize(resolved)) {
            cursor = cursor.resolve(part);
            if (Files.exists(cursor, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(cursor)) {
                throw new IllegalArgumentException("Symlink access is not allowed: " + rawPath);
            }
        }
    }
}
