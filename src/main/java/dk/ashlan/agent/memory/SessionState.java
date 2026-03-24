package dk.ashlan.agent.memory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SessionState {
    private final String sessionId;
    private final List<String> messages = new ArrayList<>();
    private final Consumer<SessionState> onChange;

    public SessionState(String sessionId) {
        this(sessionId, List.of(), null);
    }

    SessionState(String sessionId, List<String> initialMessages, Consumer<SessionState> onChange) {
        this.sessionId = sessionId;
        this.onChange = onChange;
        this.messages.addAll(initialMessages);
    }

    public String sessionId() {
        return sessionId;
    }

    public synchronized List<String> messages() {
        return List.copyOf(messages);
    }

    public void addMessage(String message) {
        Consumer<SessionState> callback;
        synchronized (this) {
            messages.add(message);
            callback = onChange;
        }
        if (callback != null) {
            // The callback runs after the mutation is committed, so persistence does not hold the lock.
            callback.accept(this);
        }
    }

    public synchronized int size() {
        return messages.size();
    }
}
