# Chapter 06 - Memory

This chapter maps the Python memory and session model into Java services and strategies.

## Python Files

- `scratch_agents/memory/base_memory_strategy.py`
- `scratch_agents/memory/core_memory_strategy.py`
- `scratch_agents/memory/sliding_window_strategy.py`
- `scratch_agents/memory/summarization_strategy.py`
- `scratch_agents/sessions/base_session_manager.py`
- `scratch_agents/sessions/base_cross_session_manager.py`
- `scratch_agents/sessions/in_memory_session_manager.py`
- `scratch_agents/sessions/session.py`
- `scratch_agents/sessions/task_cross_session_manager.py`
- `scratch_agents/sessions/user_cross_session_manager.py`
- `chapter_06_memory/*`

## Quarkus Classes

- `dk.ashlan.agent.memory.MemoryStrategy`
- `dk.ashlan.agent.memory.CoreMemoryStrategy`
- `dk.ashlan.agent.memory.SlidingWindowStrategy`
- `dk.ashlan.agent.memory.SummarizationStrategy`
- `dk.ashlan.agent.sessions.Session`
- `dk.ashlan.agent.sessions.SessionManager`
- `dk.ashlan.agent.sessions.InMemorySessionManager`
- `dk.ashlan.agent.sessions.CrossSessionManager`
- `dk.ashlan.agent.sessions.TaskCrossSessionManager`
- `dk.ashlan.agent.sessions.UserCrossSessionManager`
- `dk.ashlan.agent.memory.MemoryService`
- `dk.ashlan.agent.memory.MemoryAwareAgentOrchestrator`
- `dk.ashlan.agent.chapters.chapter06.*`

## Design Notes

- Session continuity stays in-memory and simple.
- Cross-session memory is split from per-session state.
- Strategy classes keep the architecture close to the Python reference.
- `SessionState` is the mutable per-session core, while `dk.ashlan.agent.sessions.Session` is the companion runtime-facing session object.
- The chapter demos are intentionally tiny and use seeded in-memory data so the memory behaviors remain easy to observe.

## Demo vs Production

- Demo: in-memory strategies and cross-session stores.
- Production placeholder: Redis or database-backed implementations.
