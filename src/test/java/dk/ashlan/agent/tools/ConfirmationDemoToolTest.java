package dk.ashlan.agent.tools;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfirmationDemoToolTest {
    @Test
    void confirmationDemoToolRequiresApprovalAndReturnsACompactResult() {
        ConfirmationDemoTool tool = new ConfirmationDemoTool();

        assertTrue(tool.definition().requiresConfirmation());
        assertEquals("confirmation-demo", tool.name());

        JsonToolResult result = tool.execute(Map.of("topic", "pause/resume"));

        assertTrue(result.success());
        assertEquals("confirmation-demo", result.toolName());
        assertTrue(result.output().contains("pause/resume"));
        assertEquals("ok", result.data().get("status"));
    }
}
