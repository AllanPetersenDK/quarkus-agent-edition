package dk.ashlan.agent.memory;

import dk.ashlan.agent.llm.LlmMessage;

import java.util.List;
import java.util.Optional;

public interface SessionStateStore {
    Optional<SessionStateSnapshot> load(String sessionId);

    void save(SessionState sessionState);

    default Optional<List<LlmMessage>> loadMessages(String sessionId) {
        return load(sessionId).map(SessionStateSnapshot::messages);
    }
}
