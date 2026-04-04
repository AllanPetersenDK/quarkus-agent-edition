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
                "mode", "progress_review",
                "analysis", "The draft is concise and on topic.",
                "needReplan", false
        ));
        JsonToolResult replanning = tool.execute(Map.of(
                "mode", "error_analysis",
                "analysis", "The plan is still missing a concrete retrieval step.",
                "needReplan", true,
                "alternativeDirection", "Gather the missing retrieval step before answering."
        ));

        assertTrue(normal.output().contains("Reflection recorded (PROGRESS REVIEW):"));
        assertTrue(normal.output().contains("The draft is concise and on topic."));
        assertTrue(normal.output().contains("ready for final answer: yes"));
        assertTrue(replanning.output().contains("Reflection recorded (ERROR ANALYSIS) (REPLAN NEEDED):"));
        assertTrue(replanning.output().contains("missing a concrete retrieval step"));
        assertTrue(replanning.output().contains("alternative direction: Gather the missing retrieval step before answering."));
        assertTrue(replanning.output().contains("replan: yes"));
    }
}
