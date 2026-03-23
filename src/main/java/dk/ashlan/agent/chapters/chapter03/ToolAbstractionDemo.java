package dk.ashlan.agent.chapters.chapter03;

import dk.ashlan.agent.tools.ToolDefinition;

public class ToolAbstractionDemo {
    public ToolDefinition run() {
        return new ToolDefinition("abstract-tool", "Demo abstraction");
    }
}
