package dk.ashlan.agent.sessions;

public interface SessionManager {
    Session getOrCreate(String sessionId);
}
