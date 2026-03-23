package dk.ashlan.agent.agents;

import dk.ashlan.agent.types.ConversationEvent;

import java.util.ArrayList;
import java.util.List;

public class ConversationTranscript {
    private final List<ConversationEvent> events = new ArrayList<>();

    public void add(ConversationEvent event) {
        events.add(event);
    }

    public List<ConversationEvent> events() {
        return List.copyOf(events);
    }
}
