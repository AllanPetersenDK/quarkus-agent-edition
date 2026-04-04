# Chapter 06 - Memory

This chapter maps the Python memory and session model into Java services and strategies.

The current Quarkus implementation now treats chapter 6 as the next active chapter track:

- Pattern 1 is request-time context optimization: `beforeLlm` trims the active request projection, but the full execution history stays intact, and `/api/runtime/context/sliding-window` now exposes the sliding-window track as a first-class demo seam
- Pattern 2 is session continuity: `SessionManager` and `SessionState` keep multi-turn state separate from memory
- Pattern 3 is structured long-term memory: compact problem-solving records are written after a run and retrieved across sessions with ranked structured lookup instead of flat string matching, now through a persistent JDBC-backed vector-like seam in runtime
- `after_run` is the canonical bridge into compact memory persistence
- explicit memory search remains a tool, while `conversation-search` and `recall-memory` are the visible retrieval seams and auto-injection is a runtime convenience backed by a hidden request-prep helper that the builder wires in for compatibility
- context projection is now directly observable through Swagger: `/api/runtime/context/optimize` shows the full request projection, strategy, and cache-friendly no-op paths, while `/api/runtime/context/sliding-window` previews the windowed track
- pause/resume for confirmation tools is an internal agent feature, not a callback trick, and pending tool calls are persisted in session state
- `POST /api/runtime/sessions/{sessionId}/resume` is the small Swagger-visible pause/resume seam, and `confirmation-demo` is the tiny approval-gated demo tool used to exercise it
- `POST /api/agent/run` also accepts `toolConfirmations` for the same chapter-6 resume bridge when an explicit session id is supplied, so the book-style pause/resume demo can stay close to the manual agent surface
- `delete-file` is the small confirmation-gated workspace deletion tool used for the chapter-6 hitl demo, not a general-purpose destructive platform feature
- delete/remove requests are deterministically preflighted into that confirmation flow, so the book test does not depend on a spontaneous tool-choice from the model

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
- `dk.ashlan.agent.memory.JdbcTaskMemoryStore`
- `dk.ashlan.agent.memory.MemoryAwareAgentOrchestrator`
- `dk.ashlan.agent.tools.ProcessLlmRequestTool`
- `dk.ashlan.agent.core.PendingToolCall`
- `dk.ashlan.agent.core.ToolConfirmation`
- `dk.ashlan.agent.chapters.chapter06.*`

## Memory Model

- Conversation history: the live `ExecutionContext` / session message stream that preserves the full run history.
- Session state: the persisted multi-turn state tracked by `SessionManager` and the chapter-6 `Session` demos.
- Short-term memory: `SlidingWindowStrategy`, `SummarizationStrategy`, and the `beforeLlm` context optimizer that project a smaller request without deleting the ground truth. The new sliding-window preview seam makes that track visible without mutating session state.
- Long-term memory: `MemoryService`, `ConversationSearchTool`, `RecallMemoryTool`, and the cross-session demo managers. Retrieval is ranked using the structured record fields, and the runtime path now persists those records through a JDBC-backed vector-like seam, so the visible tool path feels more companion/runtime-grade than a raw string dump.
- Bridge: `after_run` persists a compact memory signal after a run completes.

## Design Notes

- Session continuity is now persisted through H2-backed session state, so restart-like reloads preserve messages.
- Pending tool confirmations are part of that persisted session state, so pause/resume survives reloads too.
- The no-arg memory session path now uses an explicit in-memory store instead of a null-based fallback, which keeps the dev/test path clear.
- CDI runtime resolves the JDBC-backed task-memory store, while the in-memory store remains the explicit default path for manual construction and tests.
- Cross-session memory is split from per-session state, and retrieval does not depend on session continuity.
- Strategy classes keep the architecture close to the Python reference.
- `SessionState` is the mutable per-session core, while `dk.ashlan.agent.sessions.Session` is the companion runtime-facing session object.
- The chapter demos are intentionally tiny and use seeded in-memory data so the memory behaviors remain easy to observe.
- `AgentOrchestrator` now exposes a small callback seam, and the canonical runtime bridge into memory persistence lives in `after_run` instead of the core tool loop.
- `beforeLlm` now carries a small, deterministic context-optimization projection so the active request can shrink without deleting execution history, and the trace summary marks cache-friendly no-op requests versus rewrite cases.
- `ToolDefinition.requiresConfirmation()` plus `PendingToolCall` and `ToolConfirmation` form the small pause/resume bridge for confirmation-gated tools.
- `MemoryAwareAgentOrchestrator` remains as a thin chapter-6 façade, but the actual memory persistence hook is now callback-driven.
- `ConversationSearchTool` and `RecallMemoryTool` are the explicit memory retrieval tools, while automatic memory injection now routes through a hidden/internal `process_llm_request`-style request-prep seam and still keeps the builder fallback for compatibility.
- Long-term memory is stored as compact problem-solving records with `taskSummary`, `approach`, `finalAnswer`, and small correctness/error fields when they are available, and retrieval ranks those records using the structured fields rather than the raw memory string alone. The runtime store keeps that shape in JDBC so the memory layer is observable as a real persistence seam rather than a flat demo cache.
- Dedup is structured as well: exact and near-duplicate writes are suppressed with a compact dedup key and token overlap, so the store behaves like a long-term memory layer rather than a raw transcript cache.
- `ConfirmationDemoTool` is a chapter-6 demo tool only; it exists to make the pause/resume flow visible without turning approval gating into a broad runtime policy.
- The manual REST chapter-6 path treats missing session ids as ephemeral/anonymized. Direct core convenience calls still keep the compatibility default-session fallback for internal callers, so they are not guaranteed to be anonymous-safe. That distinction is deliberate and keeps the API seam safer without widening the core orchestration surface.

## Demo vs Production

- Demo: in-memory strategies and cross-session stores.
- Persistent layer: file-based H2 for session continuity and the JDBC-backed task-memory seam.
- Still in-memory: RAG chunk store, cross-session demo stores, strategy-level demo helpers, and manual chapter demos that want explicit lightweight fixtures.
- The chapter-6 memory bridge is intentionally small: it prepares the repo for further memory iterations without turning it into a separate platform.
