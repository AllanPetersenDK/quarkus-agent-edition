package dk.ashlan.agent.tools;

import java.util.Map;

public class ToolDecorator implements Tool {
    private final Tool delegate;
    private final String prefix;

    public ToolDecorator(Tool delegate, String prefix) {
        this.delegate = delegate;
        this.prefix = prefix;
    }

    @Override
    public String name() {
        return delegate.name();
    }

    @Override
    public ToolDefinition definition() {
        return delegate.definition();
    }

    @Override
    public JsonToolResult execute(Map<String, Object> arguments) {
        JsonToolResult result = delegate.execute(arguments);
        return new JsonToolResult(result.toolName(), result.success(), prefix + result.output(), result.data());
    }
}
