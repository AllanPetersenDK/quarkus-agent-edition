package dk.ashlan.agent.chapters.chapter03;

import dk.ashlan.agent.tools.CalculatorTool;
import dk.ashlan.agent.tools.ToolDecorator;

import java.util.Map;

public class ToolDecoratorWrapperDemo {
    public String run() {
        return new ToolDecorator(new CalculatorTool(), "[wrapped] ")
                .execute(Map.of("expression", "25 * 4"))
                .output();
    }
}
