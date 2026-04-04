package dk.ashlan.agent.planning;

import dk.ashlan.agent.tools.AbstractTool;
import dk.ashlan.agent.tools.ToolDefinition;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class CreateTasksTool extends AbstractTool {
    @Override
    public String name() {
        return "create-tasks";
    }

    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(
                name(),
                """
                        Chapter 7 planning tool for simple task lists.

                        WHEN TO USE:
                        - multi-step research
                        - tasks that need several information sources
                        - prompts where the agent may lose direction

                        WHEN NOT TO USE:
                        - simple one-shot questions
                        - obvious or trivial procedures

                        HOW TO USE:
                        - regenerate the whole task list
                        - mark completed tasks as completed
                        - mark the next active task as in_progress
                        - keep future tasks as pending
                        """
        );
    }

    @Override
    protected String executeSafely(Map<String, Object> arguments) {
        String goal = text(arguments.get("goal"), arguments.get("topic"), arguments.get("task"));
        List<TaskItem> tasks = parseTasks(arguments.get("tasks"));
        if (tasks.isEmpty()) {
            tasks = List.of(new TaskItem("No tasks supplied.", TaskStatus.PENDING));
        }

        StringBuilder builder = new StringBuilder("Task plan");
        if (!goal.isBlank()) {
            builder.append("\nGoal: ").append(goal);
        }
        for (TaskItem task : tasks) {
            builder.append("\n").append(task.formattedLine());
        }
        return builder.toString();
    }

    private List<TaskItem> parseTasks(Object rawTasks) {
        if (!(rawTasks instanceof Iterable<?> iterable)) {
            return List.of();
        }
        List<TaskItem> tasks = new ArrayList<>();
        for (Object candidate : iterable) {
            TaskItem task = TaskItem.from(candidate);
            if (!task.content().isBlank()) {
                tasks.add(task);
            }
        }
        return tasks;
    }

    private String text(Object... values) {
        for (Object value : values) {
            if (value != null && !value.toString().isBlank()) {
                return value.toString().trim();
            }
        }
        return "";
    }
}
