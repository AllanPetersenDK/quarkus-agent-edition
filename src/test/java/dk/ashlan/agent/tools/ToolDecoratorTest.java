package dk.ashlan.agent.tools;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolDecoratorTest {
    @Test
    void decoratorPrefixesOutput() {
        ToolDecorator decorator = new ToolDecorator(new CalculatorTool(), "[wrapped] ");

        assertTrue(decorator.execute(Map.of("expression", "25 * 4")).output().contains("[wrapped]"));
    }
}
