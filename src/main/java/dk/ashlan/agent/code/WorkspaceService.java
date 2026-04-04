package dk.ashlan.agent.code;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@ApplicationScoped
public class WorkspaceService {
    private final Path root;

    public WorkspaceService(@ConfigProperty(name = "code.workspace-root", defaultValue = "target/workspace") String workspaceRoot) {
        this.root = initializeRoot(Path.of(workspaceRoot));
    }

    public Path root() {
        return root;
    }

    public Path resolve(String relativePath) {
        Path resolved = root.resolve(relativePath).toAbsolutePath().normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("Path traversal is not allowed: " + relativePath);
        }
        rejectSymlinkHop(resolved, relativePath);
        return resolved;
    }

    public String read(String relativePath) {
        try {
            return Files.readString(resolve(relativePath));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read file: " + relativePath, exception);
        }
    }

    public void write(String relativePath, String contents) {
        try {
            Path path = resolve(relativePath);
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.writeString(path, contents, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to write file: " + relativePath, exception);
        }
    }

    public List<String> listFiles() {
        return listFiles("", true, Integer.MAX_VALUE);
    }

    public List<String> listFiles(String relativePath, boolean recursive, int maxEntries) {
        Path start = relativePath == null || relativePath.isBlank()
                ? root
                : resolve(relativePath);
        if (!Files.exists(start, LinkOption.NOFOLLOW_LINKS)) {
            return List.of();
        }
        int limit = Math.max(1, maxEntries);
        try {
            if (Files.isRegularFile(start, LinkOption.NOFOLLOW_LINKS)) {
                return List.of(root.relativize(start).toString());
            }
            Stream<Path> stream = recursive ? Files.walk(start) : Files.list(start);
            try (stream) {
                List<String> files = new ArrayList<>();
                stream.filter(path -> !path.equals(start))
                        .filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                        .filter(path -> !Files.isSymbolicLink(path))
                        .sorted()
                        .limit(limit)
                        .forEach(path -> files.add(root.relativize(path.toAbsolutePath().normalize()).toString()));
                return files;
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to list files: " + relativePath, exception);
        }
    }

    public long fileCount() {
        return listFiles().size();
    }

    private Path initializeRoot(Path configuredRoot) {
        Path absoluteRoot = configuredRoot.toAbsolutePath().normalize();
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

    private void rejectSymlinkHop(Path resolved, String relativePath) {
        Path cursor = root;
        for (Path part : root.relativize(resolved)) {
            cursor = cursor.resolve(part);
            if (Files.exists(cursor, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(cursor)) {
                throw new IllegalArgumentException("Symlink access is not allowed: " + relativePath);
            }
        }
    }
}
