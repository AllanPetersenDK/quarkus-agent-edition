package dk.ashlan.agent.code;

import dk.ashlan.agent.memory.SessionTraceStore;
import jakarta.inject.Inject;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class CodeWorkspaceRegistry {
    private final Path baseRoot;
    private final SessionTraceStore sessionTraceStore;
    private final Map<String, CodeWorkspaceSession> sessions = new ConcurrentHashMap<>();

    public CodeWorkspaceRegistry(
            @ConfigProperty(name = "code.chapter8.workspace-root", defaultValue = "target/chapter-08-workspaces")
            String workspaceRoot
    ) {
        this(workspaceRoot, null);
    }

    @Inject
    public CodeWorkspaceRegistry(
            @ConfigProperty(name = "code.chapter8.workspace-root", defaultValue = "target/chapter-08-workspaces")
            String workspaceRoot,
            SessionTraceStore sessionTraceStore
    ) {
        this.baseRoot = initializeBaseRoot(Path.of(workspaceRoot));
        this.sessionTraceStore = sessionTraceStore;
    }

    public CodeWorkspaceSession session(String sessionId) {
        String safeSessionId = safeSessionId(sessionId);
        return sessions.computeIfAbsent(safeSessionId, key -> new CodeWorkspaceSession(
                sessionId == null || sessionId.isBlank() ? safeSessionId : sessionId,
                safeSessionId,
                baseRoot.resolve(safeSessionId),
                sessionTraceStore
        ));
    }

    public Path baseRoot() {
        return baseRoot;
    }

    private Path initializeBaseRoot(Path configuredRoot) {
        Path absoluteRoot = configuredRoot.toAbsolutePath().normalize();
        try {
            if (Files.exists(absoluteRoot, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(absoluteRoot)) {
                throw new IllegalStateException("Chapter 8 workspace root symlink access is not allowed: " + absoluteRoot);
            }
            Files.createDirectories(absoluteRoot);
            return absoluteRoot.toRealPath();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to initialize chapter 8 workspace root: " + absoluteRoot, exception);
        }
    }

    private String safeSessionId(String sessionId) {
        String value = sessionId == null || sessionId.isBlank() ? "default" : sessionId.trim();
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
