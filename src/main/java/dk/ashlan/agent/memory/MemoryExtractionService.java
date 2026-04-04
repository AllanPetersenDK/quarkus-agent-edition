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
            String value = extractTail(normalized, lowered, "my name is ");
            return MemoryExtractionResult.add(structured(sessionId, task, "User name: " + value, "name", "name disclosure", value, null, null), "name");
        }
        if (lowered.contains("i live in ")) {
            String value = extractTail(normalized, lowered, "i live in ");
            return MemoryExtractionResult.add(structured(sessionId, task, "User location: " + value, "location", "location disclosure", value, null, null), "location");
        }
        if (lowered.contains("remember that ")) {
            String value = extractTail(normalized, lowered, "remember that ");
            return MemoryExtractionResult.add(structured(sessionId, task, value, "remember", "explicit remember instruction", value, null, null), "remember");
        }
        if (lowered.contains("my favorite ")) {
            String value = extractTail(normalized, lowered, "my favorite ");
            return MemoryExtractionResult.add(structured(sessionId, task, "User preference: " + value, "preference", "preference disclosure", value, null, null), "preference");
        }
        if (lowered.contains("i prefer ")) {
            String value = extractTail(normalized, lowered, "i prefer ");
            return MemoryExtractionResult.add(structured(sessionId, task, "User preference: " + value, "preference", "preference disclosure", value, null, null), "preference");
        }
        if (lowered.contains("i work with ")) {
            String value = extractTail(normalized, lowered, "i work with ");
            return MemoryExtractionResult.add(structured(sessionId, task, "User work context: " + value, "work-context", "work context disclosure", value, null, null), "work-context");
        }
        if (lowered.contains("i use ")) {
            String value = extractTail(normalized, lowered, "i use ");
            return MemoryExtractionResult.add(structured(sessionId, task, "User tool preference: " + value, "tool-preference", "tool preference disclosure", value, null, null), "tool-preference");
        }
        if (lowered.contains("i like ")) {
            String value = extractTail(normalized, lowered, "i like ");
            return MemoryExtractionResult.add(structured(sessionId, task, "User interest: " + value, "interest", "interest disclosure", value, null, null), "interest");
        }
        if (lowered.contains("i enjoy ")) {
            String value = extractTail(normalized, lowered, "i enjoy ");
            return MemoryExtractionResult.add(structured(sessionId, task, "User interest: " + value, "interest", "interest disclosure", value, null, null), "interest");
        }
        if (lowered.contains("the answer is ") || lowered.contains("the result is ")) {
            String value = extractAnswer(normalized);
            return MemoryExtractionResult.add(structured(sessionId, task, compactAnswer(normalized), "answer", "answer declaration", value, null, null), "answer");
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
        StringBuilder finalAnswer = new StringBuilder();
        if (name != null && !name.isBlank()) {
            compact.append("User name: ").append(name.trim());
            finalAnswer.append(name.trim());
        }
        if (work != null && !work.isBlank()) {
            if (compact.length() > 0) {
                compact.append("; ");
                finalAnswer.append(" / ");
            }
            compact.append("User work context: ").append(work.trim());
            finalAnswer.append(work.trim());
        }
        return MemoryExtractionResult.add(structured(
                sessionId,
                task,
                compact.toString(),
                "danish-profile",
                "explicit profile statement",
                finalAnswer.toString(),
                null,
                null
        ), "danish-profile");
    }

    private TaskMemory structured(
            String sessionId,
            String task,
            String memory,
            String taskSummary,
            String approach,
            String finalAnswer,
            Boolean correct,
            String errorAnalysis
    ) {
        return new TaskMemory(sessionId, task, memory, taskSummary, approach, finalAnswer, correct, errorAnalysis);
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

    private String extractTail(String message, String lowered, String marker) {
        int index = lowered.indexOf(marker);
        if (index < 0) {
            return "";
        }
        return message.substring(index + marker.length()).trim();
    }

    private String compactAnswer(String message) {
        String normalized = normalize(message);
        String[] pieces = normalized.split("(?i)\\b(?:the answer is|the result is)\\b", 2);
        if (pieces.length < 2) {
            return normalized;
        }
        return "Observed answer: " + pieces[1].trim();
    }

    private String extractAnswer(String message) {
        String normalized = normalize(message);
        int traceIndex = normalized.toLowerCase(Locale.ROOT).indexOf(" | trace:");
        if (traceIndex >= 0) {
            normalized = normalized.substring(0, traceIndex).trim();
        }
        int arrowIndex = normalized.indexOf("=>");
        if (arrowIndex >= 0) {
            normalized = normalized.substring(arrowIndex + 2).trim();
        }
        String[] pieces = normalized.split("(?i)\\b(?:the answer is|the result is)\\b", 2);
        if (pieces.length < 2) {
            return normalized;
        }
        return pieces[1].trim();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }
}
