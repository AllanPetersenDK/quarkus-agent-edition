package dk.ashlan.agent.chapters.chapter06;

import dk.ashlan.agent.sessions.TaskCrossSessionManager;

import java.util.List;

public class TaskLongTermDemo {
    public List<String> run() {
        TaskCrossSessionManager manager = Chapter06Support.taskCrossSessionManager();
        manager.remember("task-1", "remember quarkus");
        manager.remember("task-2", "remember java");
        return manager.search("quarkus", 10);
    }
}
