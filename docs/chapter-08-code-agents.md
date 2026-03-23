# Chapter 8 - Code Agents

## Chapter Goal

Translate the code-agent pattern into a safe workspace-oriented Java service.

## Quarkus Translation

The code agent is built around a workspace root and explicit file tools, with path traversal prevention as a first-class concern.

## Central Classes

- `dk.ashlan.agent.code.WorkspaceService`
- `dk.ashlan.agent.code.FileReadTool`
- `dk.ashlan.agent.code.FileWriteTool`
- `dk.ashlan.agent.code.TestExecutionTool`
- `dk.ashlan.agent.code.CodeGenerationTool`
- `dk.ashlan.agent.code.CodeAgentOrchestrator`

## Design Choices

- All file access stays under the workspace root.
- The code-generation and test-execution pieces are placeholders in the companion edition.
- Safety is validated by dedicated tests.

## Demo vs Production

- Demo: generated text files and placeholder test execution.
- Production placeholders: actual patch generation and sandboxed command execution.
