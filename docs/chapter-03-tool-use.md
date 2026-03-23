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
- `dk.ashlan.agent.tools.WebSearchTool`
- `dk.ashlan.agent.chapters.chapter03.*`

## Design Notes

- Tools are CDI beans and can be discovered generically.
- The registry stays generic so new tools can be added without rewriting the executor.
- Wikipedia and decorator examples are intentionally lightweight demo ports.

## Demo vs Production

- Demo: calculator and placeholder web search.
- Production placeholder: external search providers and richer tool schema generation.
