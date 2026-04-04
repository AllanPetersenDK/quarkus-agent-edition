package dk.ashlan.agent.planning;

import java.util.Map;

public record TaskItem(String content, TaskStatus status) {
    public TaskItem {
        content = content == null ? "" : content.trim();
        status = status == null ? TaskStatus.PENDING : status;
    }

    public static TaskItem from(Object value) {
        if (value instanceof TaskItem taskItem) {
            return taskItem;
        }
        if (value instanceof Map<?, ?> map) {
            return new TaskItem(
                    text(map.get("content"), map.get("task"), map.get("description")),
                    TaskStatus.from(map.get("status"))
            );
        }
        return new TaskItem(value == null ? "" : value.toString(), TaskStatus.PENDING);
    }

    public String formattedLine() {
        return switch (status) {
            case IN_PROGRESS -> "[>] **" + content + "**";
            case COMPLETED -> "[x] ~~" + content + "~~";
            case PENDING -> "[ ] " + content;
        };
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
