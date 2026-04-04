# Chapter Status

## Mapped

- Chapter 02 LLM
- Chapter 03 Tool Use
- Chapter 04 Basic Agent
- Chapter 05 RAG
- Chapter 06 Memory
- Chapter 07 Planning and Reflection
- Chapter 08 Code Agents
- Chapter 09 Multi-Agent
- Chapter 10 Evaluation and Monitoring

## Quarkus Companion Extensions

- `langchain4j`
- `mcp`
- `rag`
- `planning`
- `code`
- `multiagent`
- `eval`

## Notes

- The Python reference zip remains the source-of-truth for chapter progression and file-level mapping.
- Quarkus-native modules are documented separately as companion extensions.
- Chapter 2 is mature as the LLM foundation, chapter 3 is mature as the generic tool system, and chapter 4 is mature as the manual agent loop; later chapters should be read as extensions on top of that core rather than as part of the core loop itself.
- Chapter 7 is now the next active track: planning and reflection are layered directly on the chapter 2-4 manual core and the chapter 6 memory/context foundation. Chapter 6 still underpins Pattern 1 request-time context optimization, Pattern 2 session continuity, and Pattern 3 structured long-term memory, including the sliding-window preview seam.
- Chapter 7 is now runtime-visible and Swagger-bevisbar in the current build: the runtime exposes the task-plan tool, the reflection tool, plan/reflection inspection seams, and an explicit plan/reflection/replan trace story. The chapter-7 cycle is still lightweight, but it is now materially book-faithful and inspectable rather than just a demo overlay.
- Chapter 8 is now a session-scoped workspace/code-agent companion: `POST /api/code-agent/run` writes workspace-local artifacts, registers a narrow generated tool, and exposes workspace and generated-tool inspection seams so the code-agent story is Swagger-visible without turning into a workflow engine.
- Chapter 9 is now a small but real multi-agent runtime seam: `/multi-agent` exposes coordinator-led delegation, deterministic routing, reviewer judgment, and a compact run history so the flow is inspectable after execution instead of only in the live response body.
- The first product lane now exists under `dk.ashlan.agent.product` and `POST /api/v1/assistants/query`. It is a small, conversation-aware document/knowledge assistant built on RAG, memory, planning, reflection, and simple observability metadata. Chapter demos still exist for the book companion story, but they are no longer the most natural product entrypoint.
- Chapter 10 is now the shared runtime observability and evaluation layer: `GET /api/runtime/runs` and `GET /api/runtime/runs/{runId}` make the manual, product, code-agent, multi-agent, evaluation, and GAIA lanes inspectable after execution, while `POST /admin/evaluations/runs` provides a lightweight case-based evaluation seam that stays repo-nær instead of becoming a full monitoring platform. The chapter-10 records are intentionally compact, with run id, lane, trace summary, quality signals, approval/rejection details, and failure reasons exposed so a run can be replayed or explained after the fact.
- The memory story is companion/runtime-grade rather than a new platform: explicit retrieval tools and auto-injection coexist, after-run persistence remains the canonical write bridge, and Pattern 3 is structured enough to feel like long-term memory rather than a flat demo cache. The runtime task-memory path is now JDBC-backed and vector-like, powered by embeddings rather than only an in-memory demo store, and a tiny Quarkus Cache now wraps the pure ranking helper for stable `TaskMemory` + query inputs. The retrieval layer is still an in-process scoring pass rather than a dedicated vector index.
- Runtime defaults now use H2-backed persistence for session state and RAG chunks, while the chapter demos still lean on deterministic in-memory stand-ins.
- Context optimization is directly observable in Swagger because the projection endpoint now shows original history, projected request, strategy, and cache-friendly no-op versus rewrite behavior.
- Demo, runtime default, and production seam are treated as separate modes in the companion docs so placeholder code is not confused with the normal runtime path.
- The repo currently covers code, multi-agent, eval, MCP, and LangChain4j comparison seams, but it does not claim browser automation or a full auth story yet.
