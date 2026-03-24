# Persistence

## What Became Persistent First

- Session state for `dk.ashlan.agent.memory.SessionManager`
- Conversation messages stored through `dk.ashlan.agent.memory.SessionState`

This is the first persistence layer because session continuity is the most immediately useful restart-sensitive state in the current agent architecture.

## Why H2

- H2 gives a file-based datastore with no external infrastructure.
- It fits the current Quarkus companion app well as a first durable layer.
- It keeps the persistence story simple while the repo is still evolving.

## Local Setup

- The default database lives under `target/agent-state/agent-state`.
- Quarkus starts Flyway at application boot, so the table is created automatically.
- To reset local state, delete the `target/agent-state` directory.

## What Is Still In Memory

- RAG chunk/vector storage
- Long-term memory extraction data outside the session state
- Chapter demo stores and helper stacks that are intentionally deterministic

## Limitations

- H2 is a first persistence step, not the final production datastore.
- The current model persists session messages only, not a full relational conversation graph.
- The RAG layer still uses in-memory retrieval structures.
- There is no H2 console enabled by default, because the repo currently treats the database as a local embedded store rather than an interactive admin surface.

## Next Step

The natural next persistence candidate is either:

1. Move the RAG chunk store to a durable backend.
2. Replace H2 with PostgreSQL if the repo needs more realistic production storage.
