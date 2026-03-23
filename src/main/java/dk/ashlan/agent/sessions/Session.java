package dk.ashlan.agent.sessions;

import java.util.ArrayList;
import java.util.List;

public class Session {
    private final String id;
    private final List<String> events = new ArrayList<>();

    public Session(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public List<String> events() {
        return List.copyOf(events);
    }

    public void addEvent(String event) {
        events.add(event);
    }
}
