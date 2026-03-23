package dk.ashlan.agent.tools;

import java.util.Map;

public abstract class AbstractTool implements Tool {
    @Override
    public JsonToolResult execute(Map<String, Object> arguments) {
        try {
            return JsonToolResult.success(name(), executeSafely(arguments));
        } catch (RuntimeException exception) {
            return JsonToolResult.failure(name(), exception.getMessage());
        }
    }

    protected abstract String executeSafely(Map<String, Object> arguments);
}
