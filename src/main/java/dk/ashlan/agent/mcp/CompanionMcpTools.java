package dk.ashlan.agent.mcp;

import dk.ashlan.agent.tools.CalculatorTool;
import dk.ashlan.agent.tools.ClockTool;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CompanionMcpTools {
    private final CalculatorTool calculatorTool;
    private final ClockTool clockTool;

    public CompanionMcpTools(CalculatorTool calculatorTool, ClockTool clockTool) {
        this.calculatorTool = calculatorTool;
        this.clockTool = clockTool;
    }

    @Tool(description = "Evaluate a small arithmetic expression using the existing calculator tool.")
    public String mcpCalculator(@ToolArg(description = "Arithmetic expression to evaluate.") String expression) {
        return calculatorTool.calculate(expression);
    }

    @Tool(description = "Return the current time in ISO-8601 format using the existing clock tool.")
    public String mcpClock() {
        return clockTool.getTime();
    }
}
