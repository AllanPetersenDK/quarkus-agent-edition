# Chapter 4 - Agent Loop

## Chapter Goal

Implement the iterative loop that turns an LLM response into tool execution and then back into a final answer.

## Quarkus Translation

`AgentOrchestrator` drives the loop, `LlmRequestBuilder` prepares messages, and `ToolExecutor` resolves tool calls.

## Central Classes

- `dk.ashlan.agent.core.AgentOrchestrator`
- `dk.ashlan.agent.core.AgentRunResult`
- `dk.ashlan.agent.core.StopReason`
- `dk.ashlan.agent.core.LlmRequestBuilder`
- `dk.ashlan.agent.core.ExecutionContext`

## Design Choices

- The loop stops on a final answer or a configurable iteration limit.
- Tool calls are kept explicit so the flow is easy to trace.
- The implementation is small on purpose to keep the chapter visible.

## Demo vs Production

- Demo: `DemoToolCallingLlmClient` and the calculator/clock tools.
- Production placeholder: swapping in a real LLM provider.
