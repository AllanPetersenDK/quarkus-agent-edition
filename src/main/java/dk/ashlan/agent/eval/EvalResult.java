package dk.ashlan.agent.eval;

public record EvalResult(String caseId, boolean passed, String output, String notes) {
}
