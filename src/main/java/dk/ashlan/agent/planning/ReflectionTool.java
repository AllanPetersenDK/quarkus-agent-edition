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
        String mode = text(arguments.get("mode"), arguments.get("phase"));
        boolean needReplan = booleanValue(arguments.get("needReplan"));
        boolean readyToAnswer = arguments.containsKey("readyToAnswer")
                ? booleanValue(arguments.get("readyToAnswer"))
                : !needReplan && analysis.length() >= 20;
        StringBuilder output = new StringBuilder();
        output.append("Reflection recorded");
        if (!mode.isBlank()) {
            output.append(" (").append(mode.trim().toUpperCase().replace('_', ' ')).append(")");
        }
        if (needReplan) {
            output.append(" (REPLAN NEEDED)");
        }
        output.append(": ").append(analysis);
        output.append("\n- learned: ").append(analysis.isBlank() ? "nothing concrete yet" : analysis);
        output.append("\n- progress: ").append(needReplan ? "not sufficient" : "sufficient");
        output.append("\n- approach: ").append(needReplan ? "adjust the plan and retry" : "working");
        output.append("\n- replan: ").append(needReplan ? "yes" : "no");
        output.append("\n- ready for final answer: ").append(readyToAnswer ? "yes" : "no");
        String alternativeDirection = text(arguments.get("alternativeDirection"), arguments.get("nextStep"));
        if (!alternativeDirection.isBlank()) {
            output.append("\n- alternative direction: ").append(alternativeDirection);
        }
        return output.toString();
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
