package dk.ashlan.agent.chapters.chapter03;

import dk.ashlan.agent.tools.CalculatorTool;

public class ToolDefinitionDemo {
    public String run() {
        return new CalculatorTool().definition().description();
    }
}
