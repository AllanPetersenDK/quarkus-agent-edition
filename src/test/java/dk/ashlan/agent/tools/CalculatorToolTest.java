package dk.ashlan.agent.tools;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CalculatorToolTest {
    @Test
    void calculatorEvaluatesBasicExpression() {
        CalculatorTool calculatorTool = new CalculatorTool();

        assertTrue(calculatorTool.execute(Map.of("expression", "25 * 4")).output().contains("100"));
    }
}
