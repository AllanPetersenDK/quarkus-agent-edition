package dk.ashlan.agent.chapters.chapter03;

import dk.ashlan.agent.tools.CalculatorTool;
import dk.ashlan.agent.tools.ClockTool;
import dk.ashlan.agent.tools.FunctionToolAdapter;
import dk.ashlan.agent.tools.Tool;
import dk.ashlan.agent.tools.ToolDecorator;
import dk.ashlan.agent.tools.ToolExecutor;
import dk.ashlan.agent.tools.ToolRegistry;
import dk.ashlan.agent.tools.WebSearchTool;
import dk.ashlan.agent.tools.WikipediaTool;

import java.util.List;
import java.util.Map;

final class Chapter03Support {
    private Chapter03Support() {
    }

    static ToolRegistry registry() {
        return new ToolRegistry(List.of(
                new CalculatorTool(),
                new ClockTool(),
                new WebSearchTool(),
                new WikipediaTool()
        ));
    }

    static ToolExecutor executor() {
        return new ToolExecutor(registry());
    }

    static Tool decoratedCalculator() {
        return new ToolDecorator(new CalculatorTool(), "[decorated] ");
    }

    static FunctionToolAdapter echoTool() {
        return new FunctionToolAdapter("echo-tool", "Echo input arguments.", arguments -> String.valueOf(arguments));
    }

    static String calculatorOutput() {
        return executor().execute("calculator", Map.of("expression", "25 * 4")).output();
    }
}
