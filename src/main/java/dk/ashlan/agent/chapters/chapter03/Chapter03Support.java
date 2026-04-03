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
import dk.ashlan.agent.tools.OpenAiWebSearchService;

import java.util.List;
import java.util.Map;

final class Chapter03Support {
    private Chapter03Support() {
    }

    static ToolRegistry registry() {
        return new ToolRegistry(List.of(
                new CalculatorTool(),
                new ClockTool(),
                webSearchTool(),
                new WikipediaTool()
        ));
    }

    static WebSearchTool webSearchTool() {
        return new WebSearchTool(query -> new OpenAiWebSearchService.WebSearchResult(
                "Chapter 03 web-search demo result for: " + query,
                List.of(new OpenAiWebSearchService.WebSearchSource("Chapter 03 demo source", "https://example.com/chapter03/web-search"))
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
