package dk.ashlan.agent.sessions;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class InMemorySessionManager implements SessionManager {
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    @Override
    public Session getOrCreate(String sessionId) {
        return sessions.computeIfAbsent(sessionId, Session::new);
    }
}
