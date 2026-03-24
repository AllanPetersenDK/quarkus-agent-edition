package dk.ashlan.agent.rag;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import javax.sql.DataSource;
import java.io.PrintWriter;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcVectorStorePersistenceTest {
    @TempDir
    Path tempDir;

    @Test
    void persistsChunksAndRetrievesThemAfterRestartLikeReload() throws Exception {
        JdbcDataSource dataSource = dataSource();
        createSchema(dataSource);

        Chunker chunker = new Chunker();
        FakeEmbeddingClient embeddingClient = new FakeEmbeddingClient();
        JdbcVectorStore store = new JdbcVectorStore(dataSource);
        DocumentIngestionService ingestionService = new DocumentIngestionService(chunker, embeddingClient, store);
        Retriever retriever = new Retriever(embeddingClient, store);

        ingestionService.ingest("docs", """
                Quarkus is a fast Java framework for cloud-native applications.

                The calculator tool evaluates arithmetic expressions.
                """);

        JdbcVectorStore restartedStore = new JdbcVectorStore(dataSource);
        Retriever restartedRetriever = new Retriever(embeddingClient, restartedStore);
        List<RetrievalResult> results = restartedRetriever.retrieve("arithmetic expressions", 1);

        assertEquals(1, results.size());
        assertTrue(results.get(0).chunk().text().contains("calculator"));
        assertEquals("docs", results.get(0).chunk().sourceId());
        assertTrue(results.get(0).chunk().metadata().containsKey("source"));
    }

    @Test
    void returnsEmptyResultsForEmptyStore() throws Exception {
        JdbcDataSource dataSource = dataSource();
        createSchema(dataSource);

        JdbcVectorStore store = new JdbcVectorStore(dataSource);
        assertTrue(store.search(new double[]{1.0, 0.0}, 3).isEmpty());
    }

    @Test
    void surfacesDatastoreProblems() {
        JdbcVectorStore store = new JdbcVectorStore(new FailingDataSource());
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> store.search(new double[]{1.0}, 1));
        assertTrue(exception.getMessage().contains("Unable to search persisted RAG chunks"));
    }

    @Test
    void surfacesCorruptPersistedPayloads() throws Exception {
        JdbcDataSource dataSource = dataSource();
        createSchema(dataSource);
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    merge into rag_document_chunk (
                        chunk_id,
                        source_id,
                        chunk_index,
                        chunk_text,
                        metadata_json,
                        embedding_json
                    ) key(chunk_id)
                    values ('broken', 'docs', 0, 'text', '', 'not-json')
                    """);
        }

        JdbcVectorStore store = new JdbcVectorStore(dataSource);
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> store.search(new double[]{1.0}, 1));
        assertTrue(exception.getMessage().contains("Unable to search persisted RAG chunks"));
    }

    @Test
    void surfacesEmbeddingDimensionMismatch() throws Exception {
        JdbcDataSource dataSource = dataSource();
        createSchema(dataSource);
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    merge into rag_document_chunk (
                        chunk_id,
                        source_id,
                        chunk_index,
                        chunk_text,
                        metadata_json,
                        embedding_json
                    ) key(chunk_id)
                    values ('mismatch', 'docs', 0, 'text', '{\"source\":\"docs\"}', '[1.0]')
                    """);
        }

        JdbcVectorStore store = new JdbcVectorStore(dataSource);
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> store.search(new double[]{1.0, 0.0}, 1));
        assertTrue(exception.getMessage().contains("Unable to search persisted RAG chunks"));
    }

    private JdbcDataSource dataSource() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:file:" + tempDir.resolve("rag-store").toAbsolutePath() + ";AUTO_SERVER=FALSE;DB_CLOSE_ON_EXIT=FALSE");
        dataSource.setUser("sa");
        dataSource.setPassword("sa");
        return dataSource;
    }

    private void createSchema(JdbcDataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    create table if not exists rag_document_chunk (
                        chunk_id varchar(128) primary key,
                        source_id varchar(128) not null,
                        chunk_index integer not null,
                        chunk_text clob not null,
                        metadata_json clob not null,
                        embedding_json clob not null
                    )
                    """);
        }
    }

    private static final class FailingDataSource implements DataSource {
        @Override
        public Connection getConnection() throws SQLException {
            throw new SQLException("boom");
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            throw new SQLException("boom");
        }

        @Override
        public <T> T unwrap(Class<T> iface) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) {
            return false;
        }

        @Override
        public PrintWriter getLogWriter() {
            return null;
        }

        @Override
        public void setLogWriter(PrintWriter out) {
        }

        @Override
        public void setLoginTimeout(int seconds) {
        }

        @Override
        public int getLoginTimeout() {
            return 0;
        }

        @Override
        public Logger getParentLogger() {
            return Logger.getAnonymousLogger();
        }
    }
}
