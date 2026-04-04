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
    private static final Pattern DANISH_NAME_PATTERN = Pattern.compile("(?i)\\bmit navn er\\s+(.+?)(?:,|\\.|\\bog\\b|$)");
    private static final Pattern DANISH_WORK_PATTERN = Pattern.compile("(?i)\\bjeg arbejder som\\s+(.+?)(?:,|\\.|\\bog\\b|$)");

    public MemoryExtractionResult extract(String sessionId, String task, String message) {
        String normalized = normalize(message);
        if (normalized.isBlank()) {
            return MemoryExtractionResult.skip("blank");
        }
        if (normalized.length() < 16 && NOISE.contains(normalized.toLowerCase(Locale.ROOT))) {
            return MemoryExtractionResult.skip("generic-noise");
        }
        if (!SUMMARY_PATTERN.matcher(normalized).find()) {
            if (DANISH_NAME_PATTERN.matcher(normalized).find() || DANISH_WORK_PATTERN.matcher(normalized).find()) {
                return extractDanishProfile(sessionId, task, normalized);
            }
            return MemoryExtractionResult.skip("no-memory-signal");
        }

        String lowered = normalized.toLowerCase(Locale.ROOT);
        if (lowered.contains("mit navn er ") || lowered.contains("jeg arbejder som ")) {
            MemoryExtractionResult danishProfile = extractDanishProfile(sessionId, task, normalized);
            if (danishProfile.decision() == MemoryWriteDecision.ADD) {
                return danishProfile;
            }
        }
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

    private MemoryExtractionResult extractDanishProfile(String sessionId, String task, String message) {
        String name = captureGroup(DANISH_NAME_PATTERN, message);
        String work = captureGroup(DANISH_WORK_PATTERN, message);
        if ((name == null || name.isBlank()) && (work == null || work.isBlank())) {
            return MemoryExtractionResult.skip("unsupported-danish-pattern");
        }
        StringBuilder compact = new StringBuilder();
        if (name != null && !name.isBlank()) {
            compact.append("User name: ").append(name.trim());
        }
        if (work != null && !work.isBlank()) {
            if (compact.length() > 0) {
                compact.append("; ");
            }
            compact.append("User work context: ").append(work.trim());
        }
        return MemoryExtractionResult.add(new TaskMemory(sessionId, task, compact.toString()), "danish-profile");
    }

    private String captureGroup(Pattern pattern, String message) {
        if (pattern == null || message == null) {
            return "";
        }
        var matcher = pattern.matcher(message);
        if (!matcher.find()) {
            return "";
        }
        return matcher.groupCount() >= 1 ? matcher.group(1) : "";
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
