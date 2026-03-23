package dk.ashlan.agent.memory;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Locale;

@ApplicationScoped
public class MemoryExtractionService {
    public TaskMemory extract(String sessionId, String task, String message) {
        String lowered = message.toLowerCase(Locale.ROOT);
        if (lowered.contains("my name is ")) {
            return new TaskMemory(sessionId, task, "User name: " + message.substring(lowered.indexOf("my name is ") + 11).trim());
        }
        if (lowered.contains("i live in ")) {
            return new TaskMemory(sessionId, task, "User location: " + message.substring(lowered.indexOf("i live in ") + 10).trim());
        }
        return new TaskMemory(sessionId, task, message);
    }
}
