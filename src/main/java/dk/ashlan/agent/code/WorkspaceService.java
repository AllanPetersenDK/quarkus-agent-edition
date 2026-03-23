package dk.ashlan.agent.code;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@ApplicationScoped
public class WorkspaceService {
    private final Path root;

    public WorkspaceService(@ConfigProperty(name = "code.workspace-root", defaultValue = "workspace") String workspaceRoot) {
        this.root = Path.of(workspaceRoot).toAbsolutePath().normalize();
    }

    public Path root() {
        return root;
    }

    public Path resolve(String relativePath) {
        Path resolved = root.resolve(relativePath).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("Path traversal is not allowed: " + relativePath);
        }
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
}
