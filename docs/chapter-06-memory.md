# Chapter 6 - Memory

## Chapter Goal

Add short-term, session, and long-term memory so the agent can remember context across requests.

## Quarkus Translation

The Java version uses request context, a session manager, and a task-memory store to separate ephemeral and durable memory concerns.

## Central Classes

- `dk.ashlan.agent.core.ExecutionContext`
- `dk.ashlan.agent.memory.SessionManager`
- `dk.ashlan.agent.memory.TaskMemoryStore`
- `dk.ashlan.agent.memory.InMemoryTaskMemoryStore`
- `dk.ashlan.agent.memory.MemoryExtractionService`
- `dk.ashlan.agent.memory.MemoryService`

## Design Choices

- Session continuity is explicit and testable.
- Long-term retrieval is simple keyword-based ranking in the demo edition.
- Memory retrieval is injected into request building, so the agent loop stays clean.

## Demo vs Production

- Demo: in-memory session and task memory.
- Production placeholders: Redis or database-backed memory stores.
