# Chapter 2 - LLM Integration

## Chapter Goal

Introduce an LLM abstraction that can be swapped between a demo client and a real provider.

## Quarkus Translation

The Java version uses a CDI-managed `LlmClient` interface with:

- `DemoToolCallingLlmClient` for deterministic chapter demos
- `OpenAiLlmClient` as a real Quarkus REST Client integration seam for OpenAI Chat Completions
- `LangChain4jLlmClient` as an optional framework-backed companion seam

## Central Classes

- `dk.ashlan.agent.llm.LlmClient`
- `dk.ashlan.agent.llm.LlmCompletion`
- `dk.ashlan.agent.llm.LlmMessage`
- `dk.ashlan.agent.llm.DemoToolCallingLlmClient`
- `dk.ashlan.agent.llm.OpenAiLlmClient`
- `dk.ashlan.agent.llm.LangChain4jLlmClient`

## Design Choices

- Messages are modeled explicitly instead of passing raw strings through the app.
- The demo client is predictable so the tests can prove control flow.
- The OpenAI client is a real integration seam via Quarkus REST Client, but the demo client remains the default unless you configure `openai.api-key`.
- Tool calls now round-trip with `tool_call_id` so provider-backed tool use is protocol-correct instead of only demo-correct.
- The provider seam is selectively hardened with a transport timeout boundary plus a SmallRye retry guard, but without a fallback that could hide a real provider outage.
- The LangChain4j companion seam exists as a comparison path and is selected explicitly with `agent.llm-provider=langchain4j`.
- The companion seam now reads the standard `OPENAI_API_KEY` first, then falls back to `QUARKUS_LANGCHAIN4J_OPENAI_API_KEY` for older setups, while the manual OpenAI seam reads `openai.api-key`.

## Demo vs Production

- Demo: deterministic tool-calling based on user input.
- Production: provider-backed LLM calls via `OpenAiLlmClient`.
