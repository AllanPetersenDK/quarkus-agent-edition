package dk.ashlan.agent.persistence;

import dk.ashlan.agent.memory.JdbcSessionStateStore;
import dk.ashlan.agent.memory.SessionStateStore;
import dk.ashlan.agent.rag.JdbcVectorStore;
import dk.ashlan.agent.rag.VectorStore;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@QuarkusTest
class PersistenceWiringTest {
    @Inject
    SessionStateStore sessionStateStore;

    @Inject
    VectorStore vectorStore;

    @Test
    void runtimeChoosesJdbcBackedPersistenceBeans() {
        assertInstanceOf(JdbcSessionStateStore.class, sessionStateStore);
        assertInstanceOf(JdbcVectorStore.class, vectorStore);
    }
}
