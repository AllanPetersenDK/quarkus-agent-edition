package dk.ashlan.agent.eval.gaia;

import java.util.List;

public record GaiaAnswerPostProcessResult(
        String answer,
        boolean applied,
        String reason,
        List<String> traceEvents
) {
}
