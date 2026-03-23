package dk.ashlan.agent.types;

import java.time.Instant;

public record Event(String type, String details, Instant timestamp) {
    public static Event of(String type, String details) {
        return new Event(type, details, Instant.now());
    }
}
