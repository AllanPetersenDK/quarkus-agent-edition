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
        String normalizedMessage = normalize(message);
        String normalizedTask = normalize(task);
        if (normalizedMessage.isBlank() && normalizedTask.isBlank()) {
            return MemoryExtractionResult.skip("blank");
        }
        if (normalizedMessage.length() < 16 && NOISE.contains(normalizedMessage.toLowerCase(Locale.ROOT))) {
            return MemoryExtractionResult.skip("generic-noise");
        }
        if (!SUMMARY_PATTERN.matcher(normalizedMessage).find() && !SUMMARY_PATTERN.matcher(normalizedTask).find()) {
            if (DANISH_NAME_PATTERN.matcher(normalizedMessage).find() || DANISH_WORK_PATTERN.matcher(normalizedMessage).find()) {
                return extractDanishProfile(sessionId, task, normalizedMessage);
            }
            return MemoryExtractionResult.skip("no-memory-signal");
        }

        String loweredMessage = normalizedMessage.toLowerCase(Locale.ROOT);
        String loweredTask = normalizedTask.toLowerCase(Locale.ROOT);
        if (containsMarker(loweredMessage, loweredTask, "mit navn er ") || containsMarker(loweredMessage, loweredTask, "jeg arbejder som ")) {
            MemoryExtractionResult danishProfile = extractDanishProfile(sessionId, task, firstSource(normalizedMessage, normalizedTask, "mit navn er ", "jeg arbejder som "));
            if (danishProfile.decision() == MemoryWriteDecision.ADD) {
                return danishProfile;
            }
        }
        if (containsMarker(loweredMessage, loweredTask, "my name is ")) {
            String value = stripTrace(extractTailFromEither(normalizedMessage, normalizedTask, "my name is "));
            return MemoryExtractionResult.add(structured(sessionId, task, "User name: " + value, "name", "name disclosure", value, null, null), "name");
        }
        if (containsMarker(loweredMessage, loweredTask, "i live in ")) {
            String value = stripTrace(extractTailFromEither(normalizedMessage, normalizedTask, "i live in "));
            return MemoryExtractionResult.add(structured(sessionId, task, "User location: " + value, "location", "location disclosure", value, null, null), "location");
        }
        if (containsMarker(loweredMessage, loweredTask, "remember that ")) {
            String value = stripTrace(extractTailFromEither(normalizedMessage, normalizedTask, "remember that "));
            return MemoryExtractionResult.add(structured(sessionId, task, "User memory signal: " + value, "remember", "explicit remember instruction", value, null, null), "remember");
        }
        if (containsMarker(loweredMessage, loweredTask, "my favorite ")) {
            String value = stripTrace(extractTailFromEither(normalizedMessage, normalizedTask, "my favorite "));
            return MemoryExtractionResult.add(structured(sessionId, task, "User preference: " + value, "preference", "preference disclosure", value, null, null), "preference");
        }
        if (containsMarker(loweredMessage, loweredTask, "i prefer ")) {
            String value = stripTrace(extractTailFromEither(normalizedMessage, normalizedTask, "i prefer "));
            return MemoryExtractionResult.add(structured(sessionId, task, "User preference: " + value, "preference", "preference disclosure", value, null, null), "preference");
        }
        if (containsMarker(loweredMessage, loweredTask, "i work with ")) {
            String value = stripTrace(extractTailFromEither(normalizedMessage, normalizedTask, "i work with "));
            return MemoryExtractionResult.add(structured(sessionId, task, "User work context: " + value, "work-context", "work context disclosure", value, null, null), "work-context");
        }
        if (containsMarker(loweredMessage, loweredTask, "i use ")) {
            String value = stripTrace(extractTailFromEither(normalizedMessage, normalizedTask, "i use "));
            return MemoryExtractionResult.add(structured(sessionId, task, "User tool preference: " + value, "tool-preference", "tool preference disclosure", value, null, null), "tool-preference");
        }
        if (containsMarker(loweredMessage, loweredTask, "i like ")) {
            String value = stripTrace(extractTailFromEither(normalizedMessage, normalizedTask, "i like "));
            return MemoryExtractionResult.add(structured(sessionId, task, "User interest: " + value, "interest", "interest disclosure", value, null, null), "interest");
        }
        if (containsMarker(loweredMessage, loweredTask, "i enjoy ")) {
            String value = stripTrace(extractTailFromEither(normalizedMessage, normalizedTask, "i enjoy "));
            return MemoryExtractionResult.add(structured(sessionId, task, "User interest: " + value, "interest", "interest disclosure", value, null, null), "interest");
        }
        if (loweredMessage.contains("the answer is ") || loweredMessage.contains("the result is ")) {
            String value = extractAnswer(normalizedMessage);
            return MemoryExtractionResult.add(structured(sessionId, task, compactAnswer(normalizedMessage), "answer", "answer declaration", value, null, null), "answer");
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

    private String extractTailFromEither(String first, String second, String marker) {
        String loweredFirst = first == null ? "" : first.toLowerCase(Locale.ROOT);
        if (loweredFirst.contains(marker)) {
            return extractTail(first, loweredFirst, marker);
        }
        String loweredSecond = second == null ? "" : second.toLowerCase(Locale.ROOT);
        if (loweredSecond.contains(marker)) {
            return extractTail(second, loweredSecond, marker);
        }
        return "";
    }

    private boolean containsMarker(String first, String second, String marker) {
        return (first != null && first.contains(marker)) || (second != null && second.contains(marker));
    }

    private String firstSource(String first, String second, String... markers) {
        for (String marker : markers) {
            if (first != null && first.toLowerCase(Locale.ROOT).contains(marker)) {
                return first;
            }
            if (second != null && second.toLowerCase(Locale.ROOT).contains(marker)) {
                return second;
            }
        }
        return first;
    }

    private String stripTrace(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.trim();
        int traceIndex = normalized.toLowerCase(Locale.ROOT).indexOf(" | trace:");
        if (traceIndex >= 0) {
            normalized = normalized.substring(0, traceIndex).trim();
        }
        return normalized;
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
