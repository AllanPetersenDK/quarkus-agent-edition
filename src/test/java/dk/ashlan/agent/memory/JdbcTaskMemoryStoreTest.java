package dk.ashlan.agent.memory;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

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

    @Test
    void persistsVeryLongDedupKeysWithoutOverflowAndCollapsesExactDuplicates() throws Exception {
        JdbcDataSource dataSource = dataSource();
        JdbcTaskMemoryStore store = new JdbcTaskMemoryStore(dataSource);
        String longTrace = "After run => " + "result ".repeat(180) + " | trace: " + "trace ".repeat(120);
        TaskMemory first = new TaskMemory(
                "session-1",
                "goal",
                longTrace,
                "after-run capture",
                "after-run capture",
                "result",
                Boolean.TRUE,
                null
        );
        TaskMemory duplicate = new TaskMemory(
                "session-2",
                "goal",
                longTrace,
                "after-run capture",
                "after-run capture",
                "result",
                Boolean.TRUE,
                null
        );

        assertDoesNotThrow(() -> {
            store.save(first);
            store.save(duplicate);
        });

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     select dedup_key, length(dedup_key) as dedup_key_length, count(*) over() as row_count
                     from task_memory
                     order by dedup_key
                     fetch first 1 row only
                     """);
             ResultSet resultSet = statement.executeQuery()) {
            assertTrue(resultSet.next());
            assertTrue(resultSet.getInt("dedup_key_length") <= 512);
            assertEquals(1, resultSet.getInt("row_count"));
        }

        assertEquals(1, store.findRelevant("session-3", "result", 5).size());
    }

    private JdbcDataSource dataSource() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:file:" + tempDir.resolve("task-memory").toAbsolutePath() + ";AUTO_SERVER=FALSE;DB_CLOSE_ON_EXIT=FALSE");
        dataSource.setUser("sa");
        dataSource.setPassword("sa");
        return dataSource;
    }
}
