package dk.ashlan.agent.tools;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.LinkedHashMap;
import java.util.Map;

@ApplicationScoped
public class ConfirmationDemoTool implements Tool {
    @Override
    public String name() {
        return "confirmation-demo";
    }

    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(
                name(),
                "Confirmation-gated demo tool for chapter 6 pause/resume tests.",
                true,
                "Approve the confirmation demo tool call?"
        );
    }

    @Override
    public JsonToolResult execute(Map<String, Object> arguments) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "ok");
        data.put("arguments", arguments == null ? Map.of() : Map.copyOf(arguments));
        String topic = arguments == null ? "" : String.valueOf(arguments.getOrDefault("topic", "chapter 6"));
        return new JsonToolResult(
                name(),
                true,
                "status=ok\ntool=confirmation-demo\ntopic=" + topic + "\nresult=approved-demo-action",
                data
        );
    }
}
