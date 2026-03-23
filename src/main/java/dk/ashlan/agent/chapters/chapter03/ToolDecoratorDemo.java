package dk.ashlan.agent.chapters.chapter03;

import dk.ashlan.agent.tools.ToolDecorator;

import java.util.Map;

public class ToolDecoratorDemo {
    public String run(String input) {
        return new ToolDecorator(new dk.ashlan.agent.tools.CalculatorTool(), "[decorated] ")
                .execute(Map.of("expression", input))
                .output();
    }
}
