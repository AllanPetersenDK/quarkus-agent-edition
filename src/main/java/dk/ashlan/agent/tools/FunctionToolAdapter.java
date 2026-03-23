package dk.ashlan.agent.tools;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.function.Function;

@ApplicationScoped
public class FunctionToolAdapter extends AbstractTool {
    private final String name;
    private final String description;
    private final Function<Map<String, Object>, String> function;

    public FunctionToolAdapter() {
        this.name = "function-tool";
        this.description = "Generic function-backed tool adapter.";
        this.function = arguments -> String.valueOf(arguments);
    }

    public FunctionToolAdapter(String name, String description, Function<Map<String, Object>, String> function) {
        this.name = name;
        this.description = description;
        this.function = function;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(name(), description);
    }

    @Override
    protected String executeSafely(Map<String, Object> arguments) {
        return function.apply(arguments);
    }
}
