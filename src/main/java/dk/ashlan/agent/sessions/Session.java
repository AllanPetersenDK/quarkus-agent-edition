package dk.ashlan.agent.sessions;

import java.util.ArrayList;
import java.util.List;
import java.time.Instant;

public class Session {
    private final String id;
    private final String userId;
    private String state;
    private final Instant createdAt;
    private Instant updatedAt;
    private final List<String> events = new ArrayList<>();

    public Session(String id) {
        this(id, null, "active", Instant.now(), Instant.now());
    }

    public Session(String id, String userId, String state, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.state = state == null ? "active" : state;
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
        this.updatedAt = updatedAt == null ? this.createdAt : updatedAt;
    }

    public String id() {
        return id;
    }

    public String sessionId() {
        return id;
    }

    public String userId() {
        return userId;
    }

    public String state() {
        return state;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public List<String> events() {
        return List.copyOf(events);
    }

    public void state(String state) {
        this.state = state == null ? this.state : state;
        this.updatedAt = Instant.now();
    }

    public void addEvent(String event) {
        events.add(event);
        updatedAt = Instant.now();
    }
}
