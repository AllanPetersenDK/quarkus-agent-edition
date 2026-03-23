package dk.ashlan.agent.tools;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FunctionToolAdapterTest {
    @Test
    void functionAdapterDelegatesToProvidedFunction() {
        FunctionToolAdapter adapter = new FunctionToolAdapter(
                "echo",
                "Echo input",
                arguments -> "echo:" + arguments.get("value")
        );

        assertTrue(adapter.execute(Map.of("value", "hello")).output().contains("hello"));
    }
}
