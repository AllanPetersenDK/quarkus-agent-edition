package dk.ashlan.agent.memory;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcTaskMemoryStoreTest {
    @TempDir
    Path tempDir;

    @Test
    void persistsStructuredLongTermMemoryAcrossStoreRecreation() {
        JdbcDataSource dataSource = dataSource();
        JdbcTaskMemoryStore store = new JdbcTaskMemoryStore(dataSource);

        store.save(new TaskMemory(
                "session-1",
                "goal",
                "Problem: choose database | Result: PostgreSQL",
                "choose database",
                "preference disclosure",
                "PostgreSQL",
                Boolean.TRUE,
                null
        ));
        store.save(new TaskMemory(
                "session-2",
                "goal",
                "Problem: choose editor | Result: Neovim",
                "choose editor",
                "preference disclosure",
                "Neovim",
                Boolean.TRUE,
                null
        ));

        JdbcTaskMemoryStore restartedStore = new JdbcTaskMemoryStore(dataSource);
        List<TaskMemory> memories = restartedStore.findRelevant("session-3", "PostgreSQL", 3);

        assertTrue(memories.size() >= 1);
        assertEquals("session-1", memories.get(0).sessionId());
        assertTrue(memories.get(0).result().contains("PostgreSQL"));
        assertTrue(memories.get(0).summary().contains("choose database"));
    }

    private JdbcDataSource dataSource() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:file:" + tempDir.resolve("task-memory").toAbsolutePath() + ";AUTO_SERVER=FALSE;DB_CLOSE_ON_EXIT=FALSE");
        dataSource.setUser("sa");
        dataSource.setPassword("sa");
        return dataSource;
    }
}
