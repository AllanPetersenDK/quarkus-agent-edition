package dk.ashlan.agent.eval.gaia;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GaiaCaseFilterTest {
    @Test
    void keepsOnlyLevelOneCasesWithoutAttachments() {
        List<GaiaValidationCase> filtered = GaiaCaseFilter.level1WithoutAttachments(List.of(
                new GaiaValidationCase("1", "q1", "a1", "1", ""),
                new GaiaValidationCase("2", "q2", "a2", "2", ""),
                new GaiaValidationCase("3", "q3", "a3", "1", "attached.pdf"),
                new GaiaValidationCase("4", "q4", "a4", "Level 1", null),
                new GaiaValidationCase("5", "q5", "a5", " 1 ", "[]")
        ));

        assertEquals(List.of("1", "4", "5"), filtered.stream().map(GaiaValidationCase::taskId).toList());
    }
}
