package dk.ashlan.agent.memory;

import java.util.ArrayList;
import java.util.List;

public class SessionState {
    private final String sessionId;
    private final List<String> messages = new ArrayList<>();

    public SessionState(String sessionId) {
        this.sessionId = sessionId;
    }

    public String sessionId() {
        return sessionId;
    }

    public List<String> messages() {
        return List.copyOf(messages);
    }

    public void addMessage(String message) {
        messages.add(message);
    }
}
