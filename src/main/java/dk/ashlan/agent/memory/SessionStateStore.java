package dk.ashlan.agent.memory;

import dk.ashlan.agent.llm.LlmMessage;

import java.util.List;
import java.util.Optional;

public interface SessionStateStore {
    Optional<List<LlmMessage>> loadMessages(String sessionId);

    void save(SessionState sessionState);
}
