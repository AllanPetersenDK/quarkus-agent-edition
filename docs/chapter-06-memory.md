# Chapter 06 - Memory

This chapter maps the Python memory and session model into Java services and strategies.

The current Quarkus implementation now treats chapter 6 as the next active chapter track:

- conversation history stays in the execution/session layer
- session state keeps multi-turn continuity
- `beforeLlm` context optimization trims the active request projection
- `after_run` is the canonical bridge into compact memory persistence
- explicit memory search remains a tool, while `recall-memory` is the small explicit retrieval alias and auto-injection is a runtime convenience
- pause/resume for confirmation tools is an internal agent feature, not a callback trick, and pending tool calls are persisted in session state
- `POST /api/runtime/sessions/{sessionId}/resume` is the small Swagger-visible pause/resume seam, and `confirmation-demo` is the tiny approval-gated demo tool used to exercise it
- `POST /api/agent/run` also accepts `toolConfirmations` for the same chapter-6 resume bridge, so the book-style pause/resume demo can stay close to the manual agent surface
- `delete-file` is the small confirmation-gated workspace deletion tool used for the chapter-6 hitl demo, not a general-purpose destructive platform feature

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
- `dk.ashlan.agent.core.ContextOptimizer`
- `dk.ashlan.agent.core.callback.ContextOptimizationCallback`
- `dk.ashlan.agent.core.callback.AfterRunMemoryCallback`
- `dk.ashlan.agent.sessions.Session`
- `dk.ashlan.agent.sessions.SessionManager`
- `dk.ashlan.agent.sessions.InMemorySessionManager`
- `dk.ashlan.agent.sessions.CrossSessionManager`
- `dk.ashlan.agent.sessions.TaskCrossSessionManager`
- `dk.ashlan.agent.sessions.UserCrossSessionManager`
- `dk.ashlan.agent.memory.MemoryService`
- `dk.ashlan.agent.memory.MemoryAwareAgentOrchestrator`
- `dk.ashlan.agent.core.PendingToolCall`
- `dk.ashlan.agent.core.ToolConfirmation`
- `dk.ashlan.agent.chapters.chapter06.*`

## Memory Model

- Conversation history: the live `ExecutionContext` / session message stream that preserves the full run history.
- Session state: the persisted multi-turn state tracked by `SessionManager` and the chapter-6 `Session` demos.
- Short-term memory: `SlidingWindowStrategy`, `SummarizationStrategy`, and the `beforeLlm` context optimizer that project a smaller request without deleting the ground truth.
- Long-term memory: `MemoryService`, `ConversationSearchTool`, `RecallMemoryTool`, and the cross-session demo managers.
- Bridge: `after_run` persists a compact memory signal after a run completes.

## Design Notes

- Session continuity is now persisted through H2-backed session state, so restart-like reloads preserve messages.
- Pending tool confirmations are part of that persisted session state, so pause/resume survives reloads too.
- The no-arg memory session path now uses an explicit in-memory store instead of a null-based fallback, which keeps the dev/test path clear.
- CDI runtime still resolves the JDBC-backed store, while the in-memory store is only the explicit default path for manual construction.
- Cross-session memory is split from per-session state.
- Strategy classes keep the architecture close to the Python reference.
- `SessionState` is the mutable per-session core, while `dk.ashlan.agent.sessions.Session` is the companion runtime-facing session object.
- The chapter demos are intentionally tiny and use seeded in-memory data so the memory behaviors remain easy to observe.
- `AgentOrchestrator` now exposes a small callback seam, and the canonical runtime bridge into memory persistence lives in `after_run` instead of the core tool loop.
- `beforeLlm` now carries a small, deterministic context-optimization projection so the active request can shrink without deleting execution history.
- `ToolDefinition.requiresConfirmation()` plus `PendingToolCall` and `ToolConfirmation` form the small pause/resume bridge for confirmation-gated tools.
- `MemoryAwareAgentOrchestrator` remains as a thin chapter-6 façade, but the actual memory persistence hook is now callback-driven.
- `ConversationSearchTool` and `RecallMemoryTool` are the explicit memory retrieval tools, while automatic memory injection stays a small convenience inside the request builder.
- `ConfirmationDemoTool` is a chapter-6 demo tool only; it exists to make the pause/resume flow visible without turning approval gating into a broad runtime policy.

## Demo vs Production

- Demo: in-memory strategies and cross-session stores.
- Persistent layer: file-based H2 for session continuity.
- Still in-memory: RAG chunk store, cross-session demo stores, and strategy-level demo helpers.
- The chapter-6 memory bridge is intentionally small: it prepares the repo for further memory iterations without turning it into a separate platform.
