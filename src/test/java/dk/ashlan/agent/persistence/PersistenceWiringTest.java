package dk.ashlan.agent.persistence;

import dk.ashlan.agent.memory.JdbcSessionStateStore;
import dk.ashlan.agent.rag.JdbcVectorStore;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class PersistenceWiringTest {
    @Test
    void jdbcConstructorsCreateTheExpectedPersistenceStores() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:persistence-wiring;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("sa");

        assertInstanceOf(JdbcSessionStateStore.class, new JdbcSessionStateStore(dataSource));
        assertInstanceOf(JdbcVectorStore.class, new JdbcVectorStore(dataSource));
    }
}
