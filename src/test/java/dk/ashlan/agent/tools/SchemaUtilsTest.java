package dk.ashlan.agent.tools;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaUtilsTest {
    @Test
    void schemaUtilsFormatsDefinitions() {
        ToolRegistry registry = new ToolRegistry(List.of(new CalculatorTool()));

        assertTrue(SchemaUtils.toSchemaJson(registry.definitions()).contains("calculator"));
    }
}
