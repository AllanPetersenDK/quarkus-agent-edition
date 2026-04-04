package dk.ashlan.agent.memory;

public record TaskMemory(
        String sessionId,
        String task,
        String memory,
        String taskSummary,
        String approach,
        String finalAnswer,
        Boolean correct,
        String errorAnalysis
) {
    public TaskMemory(String sessionId, String task, String memory) {
        this(sessionId, task, memory, null, null, null, null, null);
    }

    public String problem() {
        return isBlank(taskSummary) ? task : taskSummary;
    }

    public String result() {
        return isBlank(finalAnswer) ? memory : finalAnswer;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
