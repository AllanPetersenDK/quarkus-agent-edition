package dk.ashlan.agent.planning;

import dk.ashlan.agent.tools.JsonToolResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateTasksToolTest {
    @Test
    void formatsTaskStatusesWithBookStyleMarkers() {
        CreateTasksTool tool = new CreateTasksTool();

        JsonToolResult result = tool.execute(Map.of(
                "goal", "Research Quarkus agents",
                "tasks", List.of(
                        Map.of("content", "Understand the request", "status", "pending"),
                        Map.of("content", "Draft the plan", "status", "in_progress"),
                        Map.of("content", "Ship the answer", "status", "completed")
                )
        ));

        assertTrue(result.output().contains("Goal: Research Quarkus agents"));
        assertTrue(result.output().contains("[ ] Understand the request"));
        assertTrue(result.output().contains("[>] **Draft the plan**"));
        assertTrue(result.output().contains("[x] ~~Ship the answer~~"));
    }

    @Test
    void regeneratesTheWholePlanWithoutKeepingStaleTasks() {
        CreateTasksTool tool = new CreateTasksTool();

        JsonToolResult first = tool.execute(Map.of(
                "tasks", List.of(
                        Map.of("content", "Clarify the goal", "status", "completed"),
                        Map.of("content", "Gather details", "status", "in_progress")
                )
        ));
        JsonToolResult second = tool.execute(Map.of(
                "tasks", List.of(
                        Map.of("content", "Collect evidence", "status", "completed"),
                        Map.of("content", "Write the summary", "status", "in_progress"),
                        Map.of("content", "Double-check the answer", "status", "pending")
                )
        ));

        assertTrue(first.output().contains("Clarify the goal"));
        assertTrue(second.output().contains("Collect evidence"));
        assertTrue(second.output().contains("Write the summary"));
        assertTrue(second.output().contains("Double-check the answer"));
        assertFalse(second.output().contains("Clarify the goal"));
    }
}
