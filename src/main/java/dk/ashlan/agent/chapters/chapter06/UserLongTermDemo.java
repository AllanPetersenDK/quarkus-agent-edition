package dk.ashlan.agent.chapters.chapter06;

import dk.ashlan.agent.sessions.UserCrossSessionManager;

import java.util.List;

public class UserLongTermDemo {
    public List<String> run() {
        UserCrossSessionManager manager = Chapter06Support.userCrossSessionManager();
        manager.remember("user-1", "remember java");
        manager.remember("user-2", "remember quarkus");
        return manager.search("java", 10);
    }
}
