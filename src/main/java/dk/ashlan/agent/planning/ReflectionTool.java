package dk.ashlan.agent.planning;

import dk.ashlan.agent.tools.AbstractTool;
import dk.ashlan.agent.tools.ToolDefinition;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;

@ApplicationScoped
public class ReflectionTool extends AbstractTool {
    @Override
    public String name() {
        return "reflection";
    }

    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(
                name(),
                """
                        Chapter 7 reflection tool for lightweight progress review.

                        WHEN TO USE:
                        - progress review
                        - error analysis
                        - result synthesis
                        - self check before the final answer

                        WHEN NOT TO USE:
                        - after every single tool call
                        - during trivial linear flows
                        - when the task is already simple and obvious
                        """
        );
    }

    @Override
    protected String executeSafely(Map<String, Object> arguments) {
        String analysis = text(arguments.get("analysis"), arguments.get("summary"), arguments.get("message"));
        boolean needReplan = booleanValue(arguments.get("needReplan"));
        if (needReplan) {
            return "Reflection recorded (REPLAN NEEDED): " + analysis;
        }
        return "Reflection recorded: " + analysis;
    }

    private boolean booleanValue(Object value) {
        if (value instanceof Boolean flag) {
            return flag;
        }
        return value != null && Boolean.parseBoolean(value.toString().trim());
    }

    private String text(Object... values) {
        for (Object value : values) {
            if (value != null && !value.toString().isBlank()) {
                return value.toString().trim();
            }
        }
        return "";
    }
}
