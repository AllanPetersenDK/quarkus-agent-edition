# Chapter 2 - LLM Integration

## Chapter Goal

Introduce an LLM abstraction that can be swapped between a demo client and a real provider.

## Quarkus Translation

The Java version uses a CDI-managed `LlmClient` interface with:

- `DemoToolCallingLlmClient` for deterministic chapter demos
- `OpenAiLlmClient` as a production placeholder

## Central Classes

- `dk.ashlan.agent.llm.LlmClient`
- `dk.ashlan.agent.llm.LlmCompletion`
- `dk.ashlan.agent.llm.LlmMessage`
- `dk.ashlan.agent.llm.DemoToolCallingLlmClient`
- `dk.ashlan.agent.llm.OpenAiLlmClient`

## Design Choices

- Messages are modeled explicitly instead of passing raw strings through the app.
- The demo client is predictable so the tests can prove control flow.
- The OpenAI client is a placeholder to keep the edition honest about production readiness.

## Demo vs Production

- Demo: deterministic tool-calling based on user input.
- Production placeholder: provider-backed LLM calls.
