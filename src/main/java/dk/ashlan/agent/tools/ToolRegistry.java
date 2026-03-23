package dk.ashlan.agent.tools;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class ToolRegistry {
    private final Map<String, Tool> tools = new LinkedHashMap<>();

    @Inject
    public ToolRegistry(Instance<Tool> toolInstances) {
        for (Tool tool : toolInstances) {
            tools.put(tool.name(), tool);
        }
    }

    public ToolRegistry(List<Tool> toolInstances) {
        for (Tool tool : toolInstances) {
            tools.put(tool.name(), tool);
        }
    }

    public Tool find(String name) {
        return tools.get(name);
    }

    public Map<String, ToolDefinition> definitions() {
        Map<String, ToolDefinition> definitions = new LinkedHashMap<>();
        tools.values().forEach(tool -> definitions.put(tool.name(), tool.definition()));
        return definitions;
    }
}
