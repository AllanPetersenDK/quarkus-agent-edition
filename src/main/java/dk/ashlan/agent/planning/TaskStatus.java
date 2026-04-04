package dk.ashlan.agent.planning;

public enum TaskStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED;

    public static TaskStatus from(Object value) {
        if (value == null) {
            return PENDING;
        }
        String normalized = value.toString().trim().toLowerCase();
        return switch (normalized) {
            case "in_progress", "in-progress", "in progress" -> IN_PROGRESS;
            case "completed", "done", "complete" -> COMPLETED;
            default -> PENDING;
        };
    }
}
