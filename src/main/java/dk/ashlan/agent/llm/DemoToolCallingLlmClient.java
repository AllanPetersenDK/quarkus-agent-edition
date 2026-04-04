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

        if (reflectsAfterReplan(messages)) {
            String reflection = lastToolContent(messages, "reflection", "Reflection recorded.");
            return LlmCompletion.answer("Planning/reflection complete: " + reflection);
        }
        if (needsReplanAfterReflection(messages)) {
            return LlmCompletion.toolCalls(List.of(new LlmToolCall(
                    "create-tasks",
                    Map.of(
                            "goal", latestUserMessage,
                            "tasks", buildReplanTasks(latestUserMessage)
                    )
            )));
        }
        if (needsFailureRecovery(lower) && hasToolMessage(messages, "create-tasks") && !hasToolMessage(messages, "calculator")) {
            String expression = extractExpression(latestUserMessage);
            if (expression.isBlank()) {
                expression = "1 / 0";
            }
            return LlmCompletion.toolCalls(List.of(new LlmToolCall(
                    "calculator",
                    Map.of("expression", expression)
            )));
        }
        if (needsReflectionAfterPlan(messages)) {
            boolean failureRecovery = hasToolOutput(messages, "calculator", output -> isFailureOutput(output));
            return LlmCompletion.toolCalls(List.of(new LlmToolCall(
                    "reflection",
                    Map.of(
                            "analysis", failureRecovery
                                    ? lastToolContent(messages, "calculator", "The tool failed before we could finish the work.")
                                    : lastToolContent(messages, "create-tasks", "The plan is ready for review."),
                            "mode", failureRecovery ? "error_analysis" : "progress_review",
                            "needReplan", failureRecovery || needsReplanHint(lower),
                            "readyToAnswer", !failureRecovery && !needsReplanHint(lower) && !isThinAnswerPrompt(lower),
                            "alternativeDirection", failureRecovery ? "Revise the failed step and try again with a valid input." : "",
                            "nextStep", failureRecovery ? "Adjust the plan and retry the failed step." : "Check the remaining steps before the final answer."
                    )
            )));
        }
        if (needsFailureRecovery(lower) && hasToolOutput(messages, "calculator", output -> isFailureOutput(output))) {
            return LlmCompletion.toolCalls(List.of(new LlmToolCall(
                    "reflection",
                    Map.of(
                            "analysis", lastToolContent(messages, "calculator", "The tool failed and needs recovery."),
                            "mode", "error_analysis",
                            "needReplan", true,
                            "readyToAnswer", false,
                            "alternativeDirection", "Revise the failed step and retry with a valid expression.",
                            "nextStep", "Rebuild the plan around the corrected input."
                    )
            )));
        }
        if (isChapterSevenPlanningRequest(lower) && toolAvailable(toolRegistry, "create-tasks")
                && !hasToolMessage(messages, "create-tasks")) {
            return LlmCompletion.toolCalls(List.of(new LlmToolCall(
                    "create-tasks",
                    Map.of(
                            "goal", latestUserMessage,
                            "tasks", buildTasks(latestUserMessage)
                    )
            )));
        }
        if (isChapterSevenReflectionRequest(lower) && toolAvailable(toolRegistry, "reflection")
                && !hasToolMessage(messages, "reflection")) {
            return LlmCompletion.toolCalls(List.of(new LlmToolCall(
                    "reflection",
                    Map.of(
                            "analysis", latestUserMessage,
                            "mode", isThinAnswerPrompt(lower) ? "self_check" : "result_synthesis",
                            "needReplan", needsReplanHint(lower),
                            "readyToAnswer", !isThinAnswerPrompt(lower) && !needsReplanHint(lower),
                            "alternativeDirection", needsReplanHint(lower) ? "Regenerate the plan and collect the missing prerequisite." : "",
                            "nextStep", isThinAnswerPrompt(lower) ? "Keep working until the answer has enough substance." : "Synthesize the result and answer."
                    )
            )));
        }
        if (needsCalculatorWork(lower) && toolAvailable(toolRegistry, "calculator") && !hasToolMessage(messages, "calculator")) {
            String expression = extractExpression(latestUserMessage);
            if (expression.isBlank()) {
                expression = needsFailureRecovery(lower) ? "1 / 0" : "25 * 4";
            }
            return LlmCompletion.toolCalls(List.of(new LlmToolCall(
                    "calculator",
                    Map.of("expression", expression)
            )));
        }
        if (looksLikeTimeQuestion(lower) && toolAvailable(toolRegistry, "clock") && !hasToolMessage(messages, "clock")) {
            return LlmCompletion.toolCalls(List.of(new LlmToolCall("clock", Map.of())));
        }
        if (messages.stream().anyMatch(message -> "tool".equals(message.role()) && "calculator".equals(message.name()))) {
            String answer = lastToolContent(messages, "calculator", "100");
            return LlmCompletion.answer("The result is " + answer);
        }
        if (messages.stream().anyMatch(message -> "tool".equals(message.role()) && "clock".equals(message.name()))) {
            String time = lastToolContent(messages, "clock", LocalTime.now().toString());
            return LlmCompletion.answer("Current time is " + time);
        }
        if (messages.stream().anyMatch(message -> "tool".equals(message.role()) && "reflection".equals(message.name()))) {
            String reflection = lastToolContent(messages, "reflection", "Reflection recorded.");
            return LlmCompletion.answer("Planning/reflection complete: " + reflection);
        }
        if (lower.contains("expand") || lower.contains("more detail")) {
            return LlmCompletion.answer("Here is a more complete answer based on the request: " + latestUserMessage);
        }
        return LlmCompletion.answer("Direct answer: " + latestUserMessage);
    }

    private boolean isChapterSevenPlanningRequest(String lower) {
        return lower.contains("task plan")
                || lower.contains("planning workflow")
                || lower.contains("plan and reflect")
                || lower.contains("multi-step")
                || lower.contains("chapter 7")
                || lower.contains("reflect on it");
    }

    private boolean isChapterSevenReflectionRequest(String lower) {
        return lower.contains("reflect") || lower.contains("reflection") || lower.contains("review") || lower.contains("self-check");
    }

    private boolean needsFailureRecovery(String lower) {
        return lower.contains("failure")
                || lower.contains("recover")
                || lower.contains("retry")
                || lower.contains("division by zero")
                || lower.contains(" 2 / 0")
                || lower.contains("1 / 0");
    }

    private boolean isFailureOutput(String output) {
        String lower = output == null ? "" : output.toLowerCase(Locale.ROOT);
        return lower.contains("error")
                || lower.contains("unsupported expression")
                || lower.contains("division by zero")
                || lower.contains("failure");
    }

    private boolean needsReplanHint(String lower) {
        return lower.contains("replan") || lower.contains("premise changed") || lower.contains("missing prerequisite") || lower.contains("uncertain");
    }

    private boolean isThinAnswerPrompt(String lower) {
        return lower.contains("thin") || lower.contains("too short") || lower.contains("premature") || lower.contains("incomplete");
    }

    private boolean needsCalculatorWork(String lower) {
        return lower.contains("25 * 4")
                || lower.matches(".*\\b\\d+\\s*[*xX/+-]\\s*\\d+\\b.*")
                || lower.contains("calculate")
                || lower.contains("divide")
                || lower.contains("multiply");
    }

    private boolean looksLikeTimeQuestion(String lower) {
        return lower.contains("time") || lower.contains("clock") || lower.contains("what is the time");
    }

    private boolean toolAvailable(ToolRegistry toolRegistry, String toolName) {
        return toolRegistry != null && toolRegistry.find(toolName) != null;
    }

    private boolean hasToolMessage(List<LlmMessage> messages, String toolName) {
        return messages.stream().anyMatch(message -> "tool".equals(message.role()) && toolName.equals(message.name()));
    }

    private boolean needsReflectionAfterPlan(List<LlmMessage> messages) {
        int createTasksIndex = lastToolIndex(messages, "create-tasks");
        if (createTasksIndex < 0) {
            return false;
        }
        int reflectionIndex = lastToolIndex(messages, "reflection");
        if (reflectionIndex > createTasksIndex) {
            return false;
        }
        return true;
    }

    private boolean needsReplanAfterReflection(List<LlmMessage> messages) {
        int reflectionIndex = lastToolIndex(messages, "reflection");
        if (reflectionIndex < 0) {
            return false;
        }
        String reflection = lastToolContent(messages, "reflection", "");
        if (!reflection.contains("REPLAN NEEDED")) {
            return false;
        }
        int createTasksIndex = lastToolIndex(messages, "create-tasks");
        return createTasksIndex < reflectionIndex;
    }

    private boolean reflectsAfterReplan(List<LlmMessage> messages) {
        int reflectionIndex = lastToolIndex(messages, "reflection");
        if (reflectionIndex < 0) {
            return false;
        }
        int createTasksIndex = lastToolIndex(messages, "create-tasks");
        return createTasksIndex > reflectionIndex && hasToolMessage(messages, "reflection");
    }

    private int lastToolIndex(List<LlmMessage> messages, String toolName) {
        for (int index = messages.size() - 1; index >= 0; index--) {
            LlmMessage message = messages.get(index);
            if ("tool".equals(message.role()) && toolName.equals(message.name())) {
                return index;
            }
        }
        return -1;
    }

    private boolean hasToolOutput(List<LlmMessage> messages, String toolName, java.util.function.Predicate<String> predicate) {
        return messages.stream()
                .filter(message -> "tool".equals(message.role()) && toolName.equals(message.name()))
                .map(LlmMessage::content)
                .filter(content -> content != null && !content.isBlank())
                .anyMatch(predicate);
    }

    private String lastToolContent(List<LlmMessage> messages, String toolName, String fallback) {
        return messages.stream()
                .filter(message -> "tool".equals(message.role()) && toolName.equals(message.name()))
                .reduce((first, second) -> second)
                .map(LlmMessage::content)
                .filter(content -> content != null && !content.isBlank())
                .orElse(fallback);
    }

    private List<Map<String, Object>> buildTasks(String latestUserMessage) {
        List<Map<String, Object>> tasks = new ArrayList<>();
        String goal = latestUserMessage == null ? "" : latestUserMessage.toLowerCase(Locale.ROOT);
        if (goal.contains("research") || goal.contains("multi-step") || goal.contains("chapter 7")) {
            tasks.add(Map.of(
                    "content", "Clarify the goal",
                    "status", "completed",
                    "doneWhen", "The user request is specific enough to plan against."
            ));
            tasks.add(Map.of(
                    "content", "Gather the relevant details",
                    "status", "in_progress",
                    "doneWhen", "The core facts or sources are available.",
                    "notes", "This is the active step."
            ));
            tasks.add(Map.of(
                    "content", "Produce the answer",
                    "status", "pending",
                    "doneWhen", "The gathered evidence supports a safe final answer."
            ));
        } else {
            tasks.add(Map.of(
                    "content", "Understand the request",
                    "status", "completed",
                    "doneWhen", "The question has been identified."
            ));
            tasks.add(Map.of(
                    "content", "Deliver a concise answer",
                    "status", "in_progress",
                    "doneWhen", "A short answer can be written without more work."
            ));
            tasks.add(Map.of(
                    "content", "Check whether a reflection pass is needed",
                    "status", "pending",
                    "notes", "Only for complex or thin outputs."
            ));
        }
        return tasks;
    }

    private List<Map<String, Object>> buildReplanTasks(String latestUserMessage) {
        List<Map<String, Object>> tasks = new ArrayList<>();
        String goal = latestUserMessage == null ? "" : latestUserMessage.toLowerCase(Locale.ROOT);
        if (goal.contains("failure") || goal.contains("recover") || goal.contains("retry") || goal.contains("division")) {
            tasks.add(Map.of(
                    "content", "Analyse the failure cause",
                    "status", "completed",
                    "doneWhen", "The failed step is identified."
            ));
            tasks.add(Map.of(
                    "content", "Adjust the approach",
                    "status", "in_progress",
                    "doneWhen", "A safer path or corrected input is available.",
                    "notes", "Switch method instead of repeating the same mistake."
            ));
            tasks.add(Map.of(
                    "content", "Retry or explain the corrected result",
                    "status", "pending",
                    "doneWhen", "The corrected step has been executed successfully."
            ));
        } else {
            tasks.add(Map.of(
                    "content", "Review what changed",
                    "status", "completed",
                    "doneWhen", "The new premise or missing prerequisite is known."
            ));
            tasks.add(Map.of(
                    "content", "Update the plan",
                    "status", "in_progress",
                    "doneWhen", "The next action reflects the revised premise."
            ));
            tasks.add(Map.of(
                    "content", "Continue toward the final answer",
                    "status", "pending",
                    "doneWhen", "The answer is supported by the updated plan."
            ));
        }
        return tasks;
    }

    private String extractExpression(String input) {
        String cleaned = input.replaceAll("[^0-9xX+\\-*/(). ]", " ").trim();
        if (cleaned.contains("25 * 4")) {
            return "25 * 4";
        }
        if (cleaned.contains("1 / 0") || cleaned.contains("2 / 0")) {
            return "1 / 0";
        }
        return cleaned;
    }
}
