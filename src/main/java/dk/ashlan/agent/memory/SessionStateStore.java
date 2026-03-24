package dk.ashlan.agent.memory;

import java.util.List;
import java.util.Optional;

public interface SessionStateStore {
    Optional<List<String>> loadMessages(String sessionId);

    void save(SessionState sessionState);
}
