package dk.ashlan.agent.memory;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class SessionManager {
    private final Map<String, SessionState> sessions = new ConcurrentHashMap<>();

    public SessionState session(String sessionId) {
        return sessions.computeIfAbsent(sessionId, SessionState::new);
    }
}
