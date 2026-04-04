package dk.ashlan.agent.planning;

public record Chapter7ReflectionState(
        String mode,
        String analysis,
        boolean accepted,
        boolean needReplan,
        boolean readyToAnswer,
        String alternativeDirection,
        String nextStep,
        String summary
) {
    public Chapter7ReflectionState {
        mode = mode == null ? "" : mode.trim();
        analysis = analysis == null ? "" : analysis.trim();
        alternativeDirection = alternativeDirection == null ? "" : alternativeDirection.trim();
        nextStep = nextStep == null ? "" : nextStep.trim();
        summary = summary == null ? "" : summary.trim();
    }
}
