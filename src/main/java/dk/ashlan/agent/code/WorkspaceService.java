package dk.ashlan.agent.code;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

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
            Files.createDirectories(path.getParent());
            Files.writeString(path, contents, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to write file: " + relativePath, exception);
        }
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
