package dk.ashlan.agent.llm.companion;

import dk.ashlan.agent.tools.CalculatorTool;
import dk.ashlan.agent.tools.ClockTool;
import dev.langchain4j.agent.tool.Tool;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class LangChain4jToolCallingCompanionTools {
    private final CalculatorTool calculatorTool;
    private final ClockTool clockTool;

    public LangChain4jToolCallingCompanionTools(CalculatorTool calculatorTool, ClockTool clockTool) {
        this.calculatorTool = calculatorTool;
        this.clockTool = clockTool;
    }

    @Tool(name = "calculator", value = "Evaluate a small arithmetic expression using the existing calculator tool.")
    public String calculator(String expression) {
        return calculatorTool.calculate(expression);
    }

    @Tool(name = "clock", value = "Return the current time in ISO-8601 format using the existing clock tool.")
    public String clock() {
        return clockTool.getTime();
    }
}
