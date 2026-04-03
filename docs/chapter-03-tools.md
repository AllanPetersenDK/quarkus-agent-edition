# Chapter 3 - Tools

## Chapter Goal

Teach how the agent can use tools as structured capabilities instead of free-form text tricks.

## Quarkus Translation

Tools are CDI beans that implement a common `Tool` contract and are discovered through a generic registry.

## Central Classes

- `dk.ashlan.agent.tools.Tool`
- `dk.ashlan.agent.tools.ToolDefinition`
- `dk.ashlan.agent.tools.ToolRegistry`
- `dk.ashlan.agent.tools.ToolExecutor`
- `dk.ashlan.agent.tools.CalculatorTool`
- `dk.ashlan.agent.tools.ClockTool`
- `dk.ashlan.agent.mcp.CompanionMcpTools`

## Design Choices

- The registry is generic so later modules can add new tools without changing the executor.
- Tool results are JSON-friendly records.
- Demo tools are deterministic and easy to assert in tests.
- The manual `ToolRegistry`/`ToolExecutor` path remains the primary chapter 3 model.
- `CompanionMcpTools` is a tiny MCP-facing comparison seam on top of the existing calculator and clock tools.

## Demo vs Production

- Demo: calculator and clock.
- Production placeholder: web search and knowledge-base tool integrations.
- Companion comparison: MCP server exposure for calculator and clock.
