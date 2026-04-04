package dk.ashlan.agent.planning;

import java.util.Map;

public record TaskItem(String content, TaskStatus status, String doneWhen, String notes) {
    public TaskItem {
        content = content == null ? "" : content.trim();
        status = status == null ? TaskStatus.PENDING : status;
        doneWhen = doneWhen == null ? "" : doneWhen.trim();
        notes = notes == null ? "" : notes.trim();
    }

    public static TaskItem from(Object value) {
        if (value instanceof TaskItem taskItem) {
            return taskItem;
        }
        if (value instanceof Map<?, ?> map) {
            return new TaskItem(
                    text(map.get("content"), map.get("task"), map.get("description")),
                    TaskStatus.from(map.get("status")),
                    text(map.get("doneWhen"), map.get("done_when"), map.get("criteria"), map.get("done when")),
                    text(map.get("notes"), map.get("note"), map.get("rationale"))
            );
        }
        return new TaskItem(value == null ? "" : value.toString(), TaskStatus.PENDING, "", "");
    }

    public String formattedLine() {
        StringBuilder builder = new StringBuilder(switch (status) {
            case IN_PROGRESS -> "[>] **" + content + "**";
            case COMPLETED -> "[x] ~~" + content + "~~";
            case PENDING -> "[ ] " + content;
        });
        if (!doneWhen.isBlank()) {
            builder.append("\n  - done when: ").append(doneWhen);
        }
        if (!notes.isBlank()) {
            builder.append("\n  - note: ").append(notes);
        }
        return builder.toString();
    }

    private static String text(Object... candidates) {
        for (Object candidate : candidates) {
            if (candidate != null && !candidate.toString().isBlank()) {
                return candidate.toString().trim();
            }
        }
        return "";
    }
}
