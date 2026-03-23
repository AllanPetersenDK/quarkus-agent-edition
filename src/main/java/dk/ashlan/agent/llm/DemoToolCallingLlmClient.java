package dk.ashlan.agent.llm;

import dk.ashlan.agent.core.ExecutionContext;
import dk.ashlan.agent.tools.ToolRegistry;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalTime;
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

    private String extractExpression(String input) {
        String cleaned = input.replaceAll("[^0-9xX+\\-*/(). ]", " ").trim();
        return cleaned.contains("25 * 4") ? "25 * 4" : cleaned;
    }
}
