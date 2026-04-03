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
- Runtime defaults now use H2-backed persistence for session state and RAG chunks, while the chapter demos still lean on deterministic in-memory stand-ins.
- Demo, runtime default, and production seam are treated as separate modes in the companion docs so placeholder code is not confused with the normal runtime path.
- The repo currently covers code, multi-agent, eval, MCP, and LangChain4j comparison seams, but it does not claim browser automation or a full auth story yet.
