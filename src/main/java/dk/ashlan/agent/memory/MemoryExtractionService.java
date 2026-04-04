package dk.ashlan.agent.memory;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@ApplicationScoped
public class MemoryExtractionService {
    private static final Set<String> NOISE = Set.of(
            "ok",
            "okay",
            "thanks",
            "thank you",
            "hi",
            "hello",
            "goodbye",
            "bye"
    );
    private static final Pattern SUMMARY_PATTERN = Pattern.compile("(?i)\\b(?:remember that|my name is|i live in|i prefer|my favorite|i work with|i use|i like|i enjoy|the answer is|the result is)\\b");

    public MemoryExtractionResult extract(String sessionId, String task, String message) {
        String normalized = normalize(message);
        if (normalized.isBlank()) {
            return MemoryExtractionResult.skip("blank");
        }
        if (normalized.length() < 16 && NOISE.contains(normalized.toLowerCase(Locale.ROOT))) {
            return MemoryExtractionResult.skip("generic-noise");
        }
        if (!SUMMARY_PATTERN.matcher(normalized).find()) {
            return MemoryExtractionResult.skip("no-memory-signal");
        }

        String lowered = normalized.toLowerCase(Locale.ROOT);
        if (lowered.contains("my name is ")) {
            return MemoryExtractionResult.add(new TaskMemory(sessionId, task, "User name: " + normalized.substring(lowered.indexOf("my name is ") + 11).trim()), "name");
        }
        if (lowered.contains("i live in ")) {
            return MemoryExtractionResult.add(new TaskMemory(sessionId, task, "User location: " + normalized.substring(lowered.indexOf("i live in ") + 10).trim()), "location");
        }
        if (lowered.contains("my favorite ")) {
            return MemoryExtractionResult.add(new TaskMemory(sessionId, task, "User preference: " + normalized.substring(lowered.indexOf("my favorite ") + 13).trim()), "preference");
        }
        if (lowered.contains("i prefer ")) {
            return MemoryExtractionResult.add(new TaskMemory(sessionId, task, "User preference: " + normalized.substring(lowered.indexOf("i prefer ") + 9).trim()), "preference");
        }
        if (lowered.contains("i work with ")) {
            return MemoryExtractionResult.add(new TaskMemory(sessionId, task, "User work context: " + normalized.substring(lowered.indexOf("i work with ") + 12).trim()), "work-context");
        }
        if (lowered.contains("i use ")) {
            return MemoryExtractionResult.add(new TaskMemory(sessionId, task, "User tool preference: " + normalized.substring(lowered.indexOf("i use ") + 6).trim()), "tool-preference");
        }
        if (lowered.contains("i like ")) {
            return MemoryExtractionResult.add(new TaskMemory(sessionId, task, "User interest: " + normalized.substring(lowered.indexOf("i like ") + 7).trim()), "interest");
        }
        if (lowered.contains("i enjoy ")) {
            return MemoryExtractionResult.add(new TaskMemory(sessionId, task, "User interest: " + normalized.substring(lowered.indexOf("i enjoy ") + 8).trim()), "interest");
        }
        if (lowered.contains("remember that ")) {
            return MemoryExtractionResult.add(new TaskMemory(sessionId, task, normalized.substring(lowered.indexOf("remember that ") + 14).trim()), "remember");
        }
        if (lowered.contains("the answer is ") || lowered.contains("the result is ")) {
            return MemoryExtractionResult.add(new TaskMemory(sessionId, task, compactAnswer(normalized)), "answer");
        }
        return MemoryExtractionResult.skip("unsupported-pattern");
    }

    private String compactAnswer(String message) {
        String normalized = normalize(message);
        String[] pieces = normalized.split("(?i)\\b(?:the answer is|the result is)\\b", 2);
        if (pieces.length < 2) {
            return normalized;
        }
        return "Observed answer: " + pieces[1].trim();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }
}
