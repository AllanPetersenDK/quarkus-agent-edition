# Chapter 8 - Code Agents

## Chapter Goal

Translate the code-agent pattern into a safe workspace-oriented Java service.

## Quarkus Translation

The code agent is built around a workspace root and explicit file tools, with path traversal prevention as a first-class concern.
In the current Quarkus companion, Chapter 8 is session-scoped and workspace-scoped: a run creates or reuses a session workspace, writes generated artifacts, registers a narrow generated tool, and exposes the resulting state through Swagger-visible inspection seams.

## Central Classes

- `dk.ashlan.agent.code.WorkspaceService`
- `dk.ashlan.agent.code.FileReadTool`
- `dk.ashlan.agent.code.FileWriteTool`
- `dk.ashlan.agent.code.TestExecutionTool`
- `dk.ashlan.agent.code.CodeGenerationTool`
- `dk.ashlan.agent.code.CodeAgentOrchestrator`
- `dk.ashlan.agent.code.CodeWorkspaceRegistry`
- `dk.ashlan.agent.code.CodeWorkspaceSession`
- `dk.ashlan.agent.code.GeneratedWorkspaceTool`
- `dk.ashlan.agent.chapters.chapter08.Chapter08Support`
- `dk.ashlan.agent.chapters.chapter08.WorkspaceSafetyDemo`
- `dk.ashlan.agent.chapters.chapter08.WorkspaceRoundTripDemo`
- `dk.ashlan.agent.chapters.chapter08.CodeAgentDemo`

## Design Choices

- All file access stays under the workspace root.
- Workspace state is session-scoped and reusable across later inspection calls.
- The code-generation and test-execution pieces are intentionally constrained companion seams.
- Generated-tool behavior is narrow and workspace-scoped, with `workspace-summary` acting as the book-aligned generated helper in the current runtime.
- Safety is validated by dedicated tests.
- Runtime inspection exposes the workspace, workspace files, and generated-tool registry so Chapter 8 stays inspectable from Swagger rather than hidden behind the demo classes alone.
- Stable trace markers surface workspace creation, file reads and writes, code generation, generated-tool registration, generated-tool invocation, and placeholder test execution.

## Demo vs Production

- Demo: generated text files, a generated workspace helper, and placeholder test execution.
- Runtime default: safe workspace-local file operations with session-scoped generated tools.
- Production placeholders: actual patch generation, sandboxed command execution, richer generated-tool flows, and endpoint-level auth around the seam.
