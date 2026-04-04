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
- Chapter 7 has moved beyond a tiny demo seam: the runtime now exposes a small task-plan tool, a reflection tool, a plan/reflection/replan trace story, a session/context-based re-entry cycle, and small plan/reflection inspection seams that are still lightweight but materially more book-faithful.
- The memory story is companion/runtime-grade rather than a new platform: explicit retrieval tools and auto-injection coexist, after-run persistence remains the canonical write bridge, and Pattern 3 is structured enough to feel like long-term memory rather than a flat demo cache. The runtime task-memory path is now JDBC-backed and vector-like, powered by embeddings rather than only an in-memory demo store, but the retrieval layer is still an in-process scoring pass rather than a dedicated vector index.
- Runtime defaults now use H2-backed persistence for session state and RAG chunks, while the chapter demos still lean on deterministic in-memory stand-ins.
- Context optimization is directly observable in Swagger because the projection endpoint now shows original history, projected request, strategy, and cache-friendly no-op versus rewrite behavior.
- Demo, runtime default, and production seam are treated as separate modes in the companion docs so placeholder code is not confused with the normal runtime path.
- The repo currently covers code, multi-agent, eval, MCP, and LangChain4j comparison seams, but it does not claim browser automation or a full auth story yet.
