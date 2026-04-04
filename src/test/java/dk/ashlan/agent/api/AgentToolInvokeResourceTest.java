package dk.ashlan.agent.api;

import dk.ashlan.agent.tools.CalculatorTool;
import dk.ashlan.agent.tools.ToolExecutor;
import dk.ashlan.agent.tools.ToolRegistry;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentToolInvokeResourceTest {
    @Test
    void resourceExposesChapterThreeToolInvokeSeam() throws Exception {
        Tag tag = AgentToolInvokeResource.class.getAnnotation(Tag.class);
        Path classPath = AgentToolInvokeResource.class.getAnnotation(Path.class);
        Method invoke = AgentToolInvokeResource.class.getMethod("invoke", dk.ashlan.agent.api.dto.ToolInvokeRequest.class);
        Operation operation = invoke.getAnnotation(Operation.class);
        Path methodPath = invoke.getAnnotation(Path.class);

        assertNotNull(tag);
        assertNotNull(classPath);
        assertNotNull(methodPath);
        assertEquals("Tool Execution", tag.name());
        assertTrue(tag.description().contains("chapter-3"));
        assertEquals("/api/agent/tools", classPath.value());
        assertEquals("/invoke", methodPath.value());
        assertNotNull(operation);
        assertTrue(operation.description().contains("chapter 3 tool-system execution seam"));
    }

    @Test
    void invokeDirectlyExecutesRegisteredToolAndRejectsUnknownTools() {
        ToolRegistry registry = new ToolRegistry(List.of(new CalculatorTool()));
        AgentToolInvokeResource resource = new AgentToolInvokeResource(registry, new ToolExecutor(registry));

        var response = resource.invoke(new dk.ashlan.agent.api.dto.ToolInvokeRequest(
                "calculator",
                Map.of("expression", "25 * 4"),
                "chapter3-tool"
        ));

        assertEquals("calculator", response.toolName());
        assertTrue(response.success());
        assertEquals("25 * 4 = 100", response.output());
        assertEquals("chapter3-tool", response.sessionId());
        assertTrue(response.data().containsKey("output"));

        assertThrows(NotFoundException.class, () -> resource.invoke(new dk.ashlan.agent.api.dto.ToolInvokeRequest(
                "missing-tool",
                Map.of(),
                "chapter3-tool"
        )));
    }
}
