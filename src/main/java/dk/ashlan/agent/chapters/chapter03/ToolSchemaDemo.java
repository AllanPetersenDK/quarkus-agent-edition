package dk.ashlan.agent.chapters.chapter03;

import dk.ashlan.agent.tools.CalculatorTool;
import dk.ashlan.agent.tools.SchemaUtils;
import dk.ashlan.agent.tools.ToolRegistry;

import java.util.List;

public class ToolSchemaDemo {
    public String run() {
        ToolRegistry registry = new ToolRegistry(List.of(new CalculatorTool()));
        return SchemaUtils.toSchemaJson(registry.definitions());
    }
}
