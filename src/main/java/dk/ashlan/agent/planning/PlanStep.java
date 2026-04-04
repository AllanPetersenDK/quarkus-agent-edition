package dk.ashlan.agent.planning;

public record PlanStep(int order, String description, TaskStatus status, String doneWhen, String notes) {
    public PlanStep(int order, String description, boolean completed) {
        this(order, description, completed ? TaskStatus.COMPLETED : TaskStatus.IN_PROGRESS, "", "");
    }

    public PlanStep(int order, String description, TaskStatus status) {
        this(order, description, status, "", "");
    }

    public PlanStep {
        description = description == null ? "" : description.trim();
        status = status == null ? TaskStatus.PENDING : status;
        doneWhen = doneWhen == null ? "" : doneWhen.trim();
        notes = notes == null ? "" : notes.trim();
    }

    public String formattedLine() {
        StringBuilder builder = new StringBuilder(switch (status) {
            case IN_PROGRESS -> "[>] **" + description + "**";
            case COMPLETED -> "[x] ~~" + description + "~~";
            case PENDING -> "[ ] " + description;
        });
        if (!doneWhen.isBlank()) {
            builder.append("\n  - done when: ").append(doneWhen);
        }
        if (!notes.isBlank()) {
            builder.append("\n  - note: ").append(notes);
        }
        return builder.toString();
    }
}
