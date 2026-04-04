# Persistence

## What Became Persistent First

- Session state for `dk.ashlan.agent.memory.SessionManager`
- Conversation messages stored through `dk.ashlan.agent.memory.SessionState`

This is the first persistence layer because session continuity is the most immediately useful restart-sensitive state in the current agent architecture.

## Hardening Review

- Write frequency: every session mutation still persists once, which is acceptable for short conversational state.
- Coupling: the persistence callback is now paired with an explicit in-memory fallback store, and Quarkus CDI selects the JDBC-backed beans in runtime while the in-memory beans remain explicit defaults for manual instantiation.
- Concurrency: `SessionState` now synchronizes access, which avoids lost updates when multiple threads touch the same session.
- Failures: corrupt payloads and datasource problems surface as explicit `IllegalStateException`s instead of being hidden.
- Storage model: JSON is still a good fit here because the persisted payload is intentionally small and session-shaped.

## Why H2

- H2 gives a file-based datastore with no external infrastructure.
- It fits the current Quarkus companion app well as a first durable layer.
- It keeps the persistence story simple while the repo is still evolving.

## Local Setup

- The default database lives under `target/agent-state/agent-state`.
- Quarkus starts Flyway at application boot, so the table is created automatically.
- To reset local state, delete the `target/agent-state` directory.

## What Is Still In Memory

- RAG chunk/vector storage in the chapter demos and local manual instantiations
- Long-term memory extraction data outside the session state
- The chapter-6 task-memory seam is persisted and vector-like, powered by embeddings, but its retrieval path is still an in-process row scan with Java scoring rather than a dedicated vector index
- Chapter demo stores and helper stacks that are intentionally deterministic

## Limitations

- H2 is a first persistence step, not the final production datastore.
- The current model persists session messages only, not a full relational conversation graph.
- The RAG layer now persists chunks and embeddings in H2 for the main runtime path, but retrieval still uses an in-process cosine-similarity scan rather than a real vector index.
- There is no H2 console enabled by default, because the repo currently treats the database as a local embedded store rather than an interactive admin surface.

## RAG Persistence

- `dk.ashlan.agent.rag.JdbcVectorStore` persists `DocumentChunk` rows and their embedding vectors.
- Retrieval stays simple: rows are read back and ranked in Java with cosine similarity.
- Embeddings remain deterministic and lightweight, which keeps the chapter mechanics visible.
- Blank or corrupt persisted metadata or embeddings now fail loudly instead of silently degrading to empty results.

## Next Step

The natural next persistence candidate is either:

1. Move the RAG chunk store to a durable backend.
2. Replace H2 with PostgreSQL if the repo needs more realistic production storage.
