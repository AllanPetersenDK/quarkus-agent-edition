# Fault Tolerance

## What Is Protected

- `dk.ashlan.agent.llm.OpenAiLlmClient#complete(...)`
- The OpenAI provider call behind `OpenAiLlmClient`
- `dk.ashlan.agent.tools.OpenAiWebSearchService#search(...)`
- The OpenAI Responses API call behind `WebSearchTool`

This is the only real external, networked integration in the current repo. The rest of the agent loop is in-process and already covered by deterministic tests.
The LangChain4j companion seam is intentionally separate and remains a comparison path, not the resilience baseline.

## Chosen Policy

| Component | Risk | Strategy | Why |
|---|---|---|---|
| `OpenAiLlmClient.complete(...)` | provider timeouts and transient upstream failures | timeout boundary + SmallRye retry guard | The provider call is external and idempotent enough to retry when the failure is clearly transient, while the timeout boundary follows the configured `openai.timeout-seconds` transport setting. |
| `OpenAiLlmClient.complete(...)` | invalid API key, malformed response, local serialization/parsing bugs | no retry, no fallback | These are permanent or local failures, so retrying would only hide the real problem. |
| `OpenAiWebSearchService.search(...)` | missing API key, malformed response, or upstream web-search failure | no retry, no fallback | The web-search tool should fail honestly so the manual runtime loop can decide whether to continue or answer without it. |

## What Is Not Protected Yet

- `WebSearchTool`
- `WikipediaTool`
- `ToolExecutor`
- orchestrator methods in `core`

Those paths are still internal demo implementations or pure dispatch code. There is no real external HTTP call there yet, so adding fault tolerance would mostly add noise.

## Metrics And Observability

- Micrometer already records agent and tool metrics in the manual loop.
- OpenTelemetry spans are explicit in the manual path, while LangChain4j AI services can participate in Quarkus observability around service and tool execution.
- The timeout boundary is handled explicitly at the transport seam, and no extra FT-specific custom counters were added.
- No fallback is used, so failed provider calls remain visible instead of being silently converted into fake success.

## Next Candidates

1. Add richer guard rails if the web-search tool starts failing often enough to justify retries or a circuit breaker.
2. Add `@CircuitBreaker` if provider traffic or upstream instability starts causing repeated failures.
3. Add persistence or caching once there is a durable external dependency to justify it.
