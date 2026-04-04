# Chapter 03 - Tool Use

This chapter maps the Python tool system into a generic Quarkus tool framework.

## Python Files and Concepts

- calculator
- Tavily search
- Wikipedia
- tool definition
- tool abstraction
- tool decorator
- `scratch_agents/tools/*`

## Quarkus Classes

- `dk.ashlan.agent.tools.Tool`
- `dk.ashlan.agent.tools.ToolDefinition`
- `dk.ashlan.agent.tools.ToolRegistry`
- `dk.ashlan.agent.tools.ToolExecutor`
- `dk.ashlan.agent.tools.CalculatorTool`
- `dk.ashlan.agent.tools.ClockTool`
- `dk.ashlan.agent.tools.WebSearchTool`
- `dk.ashlan.agent.tools.WikipediaTool`
- `dk.ashlan.agent.tools.FunctionToolAdapter`
- `dk.ashlan.agent.tools.filesystem.InspectPathTool`
- `dk.ashlan.agent.tools.filesystem.UnzipFileTool`
- `dk.ashlan.agent.tools.filesystem.ListFilesTool`
- `dk.ashlan.agent.tools.filesystem.ReadFileTool`
- `dk.ashlan.agent.tools.filesystem.ReadDocumentFileTool`
- `dk.ashlan.agent.tools.filesystem.ReadMediaFileTool`
- `dk.ashlan.agent.tools.ToolDecorator`
- `dk.ashlan.agent.tools.SchemaUtils`
- `dk.ashlan.agent.chapters.chapter03.*`
- `dk.ashlan.agent.chapters.chapter03.Chapter03Support`
- `dk.ashlan.agent.mcp.CompanionMcpTools`

## Design Notes

- Tools are CDI beans and can be discovered generically.
- The registry stays generic so new tools can be added without rewriting the executor.
- Web search now uses a narrow OpenAI Responses API integration, while Wikipedia remains a lightweight demo placeholder.
- The internal `ToolRegistry`/`ToolExecutor` path remains the primary tool model.
- `CompanionMcpTools` exposes only a tiny MCP-facing slice of the existing tool set so chapter 3 can be compared with a server-side protocol seam.
- The filesystem tools are Chapter 5-style exploration helpers, but they still plug into the same generic registry/executor path and can be used by the manual agent loop without redesign.
- Filesystem access is guarded by the canonical `code.workspace-root` shared with the code workspace tools, so zip extraction and file reads stay inside the same explicit base path. Access is read-only and symlink hops are rejected.
- `read_document_file` is the canonical attachment/document read tool, while `read_media_file` remains a compatibility alias so chapter 5 and GAIA share the same extraction foundation.
- The shared document-read layer now also covers common office-style documents (`docx`, `pptx`, `xlsx`, `ipynb`) through the same workspace-bound seam, so chapter 5 can reuse the same extraction path for more practical document workflows.

## Demo vs Production

- Demo: calculator, clock, generic function-backed tooling, and the chapter-03 web-search tool backed by OpenAI Responses API.
- Runtime default: local tool registry, adapter, and decorator flows.
- Production seam: external search providers and richer tool schema generation.
- Companion seam: MCP-exposed calculator and clock tools on top of the existing implementations.
