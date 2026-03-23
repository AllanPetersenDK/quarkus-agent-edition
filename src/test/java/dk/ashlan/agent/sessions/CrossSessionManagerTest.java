package dk.ashlan.agent.sessions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CrossSessionManagerTest {
    @Test
    void taskCrossSessionManagerStoresAndSearches() {
        TaskCrossSessionManager manager = new TaskCrossSessionManager();
        manager.remember("task-1", "remember quarkus");

        assertTrue(manager.search("quarkus", 10).get(0).contains("quarkus"));
    }

    @Test
    void userCrossSessionManagerDelegatesToSharedStorage() {
        UserCrossSessionManager manager = new UserCrossSessionManager();
        manager.remember("user-1", "remember java");

        assertTrue(manager.search("java", 10).get(0).contains("java"));
    }
}
