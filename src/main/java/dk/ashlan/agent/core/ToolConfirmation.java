package dk.ashlan.agent.core;

import java.util.Map;

public record ToolConfirmation(
        String toolCallId,
        boolean approved,
        Map<String, Object> arguments,
        String reason
) {
    public ToolConfirmation {
        arguments = arguments == null ? Map.of() : Map.copyOf(arguments);
    }

    public static ToolConfirmation approved(String toolCallId, Map<String, Object> arguments) {
        return new ToolConfirmation(toolCallId, true, arguments, null);
    }

    public static ToolConfirmation rejected(String toolCallId, String reason) {
        return new ToolConfirmation(toolCallId, false, Map.of(), reason);
    }
}
