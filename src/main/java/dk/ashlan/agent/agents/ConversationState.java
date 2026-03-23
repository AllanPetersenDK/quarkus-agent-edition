package dk.ashlan.agent.agents;

import dk.ashlan.agent.types.ContentItem;
import dk.ashlan.agent.types.Event;

import java.util.ArrayList;
import java.util.List;

public class ConversationState {
    private final List<ContentItem> contents = new ArrayList<>();
    private final List<Event> events = new ArrayList<>();

    public void addContent(ContentItem contentItem) {
        contents.add(contentItem);
    }

    public void addEvent(Event event) {
        events.add(event);
    }

    public List<ContentItem> contents() {
        return List.copyOf(contents);
    }

    public List<Event> events() {
        return List.copyOf(events);
    }
}
