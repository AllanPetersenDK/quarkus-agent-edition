package dk.ashlan.agent.planning;

import dk.ashlan.agent.tools.JsonToolResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReflectionToolTest {
    @Test
    void recordsNormalReflectionAndNeedReplanSignals() {
        ReflectionTool tool = new ReflectionTool();

        JsonToolResult normal = tool.execute(Map.of(
                "analysis", "The draft is concise and on topic.",
                "needReplan", false
        ));
        JsonToolResult replanning = tool.execute(Map.of(
                "analysis", "The plan is still missing a concrete retrieval step.",
                "needReplan", true
        ));

        assertTrue(normal.output().startsWith("Reflection recorded:"));
        assertTrue(normal.output().contains("The draft is concise and on topic."));
        assertTrue(replanning.output().contains("REPLAN NEEDED"));
        assertTrue(replanning.output().contains("missing a concrete retrieval step"));
    }
}
