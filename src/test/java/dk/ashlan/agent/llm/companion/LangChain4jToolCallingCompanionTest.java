package dk.ashlan.agent.llm.companion;

import dev.langchain4j.agent.tool.Tool;
import io.quarkiverse.langchain4j.RegisterAiService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LangChain4jToolCallingCompanionTest {
    @Test
    void serviceIsRegisteredWithTheExistingToolAdapter() {
        RegisterAiService register = LangChain4jToolCallingCompanionAssistant.class.getAnnotation(RegisterAiService.class);

        assertNotNull(register);
        assertArrayEquals(new Class<?>[]{LangChain4jToolCallingCompanionTools.class}, register.tools());
    }

    @Test
    void toolAdapterReusesTheExistingSimpleTools() {
        LangChain4jToolCallingCompanionTools tools = new LangChain4jToolCallingCompanionTools(new dk.ashlan.agent.tools.CalculatorTool(), new dk.ashlan.agent.tools.ClockTool());

        assertEquals("100", tools.calculator("25 * 4"));
        assertTrue(tools.clock().contains("T"));
    }

    @Test
    void toolMethodsAreExplicitlyNamedForTheFrameworkSeam() throws Exception {
        Method calculator = LangChain4jToolCallingCompanionTools.class.getMethod("calculator", String.class);
        Method clock = LangChain4jToolCallingCompanionTools.class.getMethod("clock");

        assertNotNull(calculator.getAnnotation(Tool.class));
        assertNotNull(clock.getAnnotation(Tool.class));
        assertEquals("calculator", calculator.getAnnotation(Tool.class).name());
        assertEquals("clock", clock.getAnnotation(Tool.class).name());
        assertTrue(Arrays.stream(LangChain4jToolCallingCompanionTools.class.getMethods()).anyMatch(method -> method.getName().equals("calculator")));
    }
}
