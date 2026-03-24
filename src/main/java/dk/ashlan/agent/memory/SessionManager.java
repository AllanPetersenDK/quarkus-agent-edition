package dk.ashlan.agent.memory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class SessionManager {
    private final Map<String, SessionState> sessions = new ConcurrentHashMap<>();
    private final SessionStateStore store;

    public SessionManager() {
        this.store = null;
    }

    @Inject
    public SessionManager(SessionStateStore store) {
        this.store = store;
    }

    public SessionState session(String sessionId) {
        return sessions.computeIfAbsent(sessionId, this::loadSessionState);
    }

    private SessionState loadSessionState(String sessionId) {
        if (store == null) {
            return new SessionState(sessionId);
        }
        List<String> messages = store.loadMessages(sessionId).orElse(List.of());
        return new SessionState(sessionId, messages, store::save);
    }
}
