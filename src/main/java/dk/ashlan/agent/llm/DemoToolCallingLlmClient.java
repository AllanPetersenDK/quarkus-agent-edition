package dk.ashlan.agent.llm;

import dk.ashlan.agent.core.ExecutionContext;
import dk.ashlan.agent.tools.ToolRegistry;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@ApplicationScoped
public class DemoToolCallingLlmClient implements LlmClient {
    @Override
    public LlmCompletion complete(List<LlmMessage> messages, ToolRegistry toolRegistry, ExecutionContext context) {
        String latestUserMessage = messages.stream()
                .filter(message -> "user".equals(message.role()))
                .map(LlmMessage::content)
                .reduce((first, second) -> second)
                .orElse(context.getInput());

        String lower = latestUserMessage.toLowerCase(Locale.ROOT);
        if (messages.stream().anyMatch(message -> "tool".equals(message.role()) && "calculator".equals(message.name()))) {
            String answer = messages.stream()
                    .filter(message -> "tool".equals(message.role()) && "calculator".equals(message.name()))
                    .reduce((first, second) -> second)
                    .map(LlmMessage::content)
                    .orElse("100");
            return LlmCompletion.answer("The result is " + answer);
        }
        if (messages.stream().anyMatch(message -> "tool".equals(message.role()) && "clock".equals(message.name()))) {
            String time = messages.stream()
                    .filter(message -> "tool".equals(message.role()) && "clock".equals(message.name()))
                    .reduce((first, second) -> second)
                    .map(LlmMessage::content)
                    .orElse(LocalTime.now().toString());
            return LlmCompletion.answer("Current time is " + time);
        }
        if (messages.stream().anyMatch(message -> "tool".equals(message.role()) && "reflection".equals(message.name()))) {
            String reflection = messages.stream()
                    .filter(message -> "tool".equals(message.role()) && "reflection".equals(message.name()))
                    .reduce((first, second) -> second)
                    .map(LlmMessage::content)
                    .orElse("Reflection recorded.");
            return LlmCompletion.answer("Planning/reflection complete: " + reflection);
        }
        if (messages.stream().anyMatch(message -> "tool".equals(message.role()) && "create-tasks".equals(message.name()))
                && toolAvailable(toolRegistry, "reflection")
                && looksLikeReflectionRequest(lower)) {
            String analysis = messages.stream()
                    .filter(message -> "tool".equals(message.role()) && "create-tasks".equals(message.name()))
                    .reduce((first, second) -> second)
                    .map(LlmMessage::content)
                    .orElse("Task plan drafted.");
            return LlmCompletion.toolCalls(List.of(new LlmToolCall(
                    "reflection",
                    Map.of(
                            "analysis", analysis,
                            "needReplan", needsReplan(lower)
                    )
            )));
        }
        if (looksLikePlanningRequest(lower)
                && toolAvailable(toolRegistry, "create-tasks")
                && messages.stream().noneMatch(message -> "tool".equals(message.role()) && "create-tasks".equals(message.name()))) {
            return LlmCompletion.toolCalls(List.of(new LlmToolCall(
                    "create-tasks",
                    Map.of(
                            "goal", latestUserMessage,
                            "tasks", buildTasks(latestUserMessage)
                    )
            )));
        }
        if (looksLikeReflectionRequest(lower) && toolAvailable(toolRegistry, "reflection")) {
            return LlmCompletion.toolCalls(List.of(new LlmToolCall(
                    "reflection",
                    Map.of(
                            "analysis", latestUserMessage,
                            "needReplan", needsReplan(lower)
                    )
            )));
        }
        if (isCalculatorRequest(latestUserMessage)) {
            return LlmCompletion.toolCalls(List.of(new LlmToolCall("calculator", Map.of("expression", extractExpression(latestUserMessage)))));
        }
        if (looksLikeTimeQuestion(lower)) {
            return LlmCompletion.toolCalls(List.of(new LlmToolCall("clock", Map.of())));
        }
        if (lower.contains("expand") || lower.contains("more detail")) {
            return LlmCompletion.answer("Here is a more complete answer based on the request: " + latestUserMessage);
        }
        return LlmCompletion.answer("Direct answer: " + latestUserMessage);
    }

    private boolean isCalculatorRequest(String input) {
        return input.contains("25 * 4") || input.matches(".*\\b\\d+\\s*[*xX/+-]\\s*\\d+\\b.*");
    }

    private boolean looksLikeTimeQuestion(String lower) {
        return lower.contains("time") || lower.contains("clock") || lower.contains("what is the time");
    }

    private boolean looksLikePlanningRequest(String lower) {
        return lower.contains("task plan")
                || lower.contains("planning workflow")
                || lower.contains("plan and reflect")
                || lower.contains("multi-step")
                || lower.contains("chapter 7");
    }

    private boolean looksLikeReflectionRequest(String lower) {
        return lower.contains("reflect") || lower.contains("reflection") || lower.contains("review");
    }

    private boolean needsReplan(String lower) {
        return lower.contains("replan") || lower.contains("retry") || lower.contains("complex") || lower.contains("uncertain");
    }

    private boolean toolAvailable(ToolRegistry toolRegistry, String toolName) {
        return toolRegistry != null && toolRegistry.find(toolName) != null;
    }

    private List<Map<String, Object>> buildTasks(String latestUserMessage) {
        List<Map<String, Object>> tasks = new ArrayList<>();
        String goal = latestUserMessage == null ? "" : latestUserMessage.toLowerCase(Locale.ROOT);
        if (goal.contains("research") || goal.contains("multi-step")) {
            tasks.add(Map.of("content", "Clarify the goal", "status", "completed"));
            tasks.add(Map.of("content", "Gather the relevant details", "status", "in_progress"));
            tasks.add(Map.of("content", "Produce the answer", "status", "pending"));
        } else {
            tasks.add(Map.of("content", "Understand the request", "status", "completed"));
            tasks.add(Map.of("content", "Deliver a concise answer", "status", "in_progress"));
            tasks.add(Map.of("content", "Check whether a reflection pass is needed", "status", "pending"));
        }
        return tasks;
    }

    private String extractExpression(String input) {
        String cleaned = input.replaceAll("[^0-9xX+\\-*/(). ]", " ").trim();
        return cleaned.contains("25 * 4") ? "25 * 4" : cleaned;
    }
}
