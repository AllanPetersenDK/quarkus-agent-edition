package dk.ashlan.agent.memory;

import dk.ashlan.agent.llm.LlmMessage;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@DefaultBean
@ApplicationScoped
public class InMemorySessionStateStore implements SessionStateStore {
    private final Map<String, List<LlmMessage>> sessions = new ConcurrentHashMap<>();

    @Override
    public Optional<List<LlmMessage>> loadMessages(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId)).map(List::copyOf);
    }

    @Override
    public void save(SessionState sessionState) {
        sessions.put(sessionState.sessionId(), sessionState.messages());
    }
}
