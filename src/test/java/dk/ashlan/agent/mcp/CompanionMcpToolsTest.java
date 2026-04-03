package dk.ashlan.agent.mcp;

import dk.ashlan.agent.tools.CalculatorTool;
import dk.ashlan.agent.tools.ClockTool;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CompanionMcpToolsTest {
    @Test
    void calculatorDelegatesToTheExistingCalculatorTool() {
        CompanionMcpTools tools = new CompanionMcpTools(new CalculatorTool(), new ClockTool());

        assertEquals("100", tools.mcpCalculator("25 * 4"));
    }

    @Test
    void clockDelegatesToTheExistingClockTool() {
        CompanionMcpTools tools = new CompanionMcpTools(new CalculatorTool(), new ClockTool());

        String clock = tools.mcpClock();

        assertFalse(clock.isBlank());
        assertDoesNotThrow(() -> OffsetDateTime.parse(clock));
    }
}
