# Companion Seams

This repository keeps the manual from-scratch path as the primary learning model.

Quarkus AI integrations are added only as clearly named companion seams for comparison:

- `OpenAiLlmClient` remains the manual provider seam used by the main agent loop.
- `LangChain4jLlmClient` is a framework-backed LLM companion seam.
- `LangChain4jToolCallingCompanionAssistant` is a framework-backed tool-calling companion seam that reuses the repo's calculator and clock tools.
- `CompanionMcpTools` exposes a tiny MCP server view over existing tools.
- `LangChain4jAgenticCompanionDemo` shows a framework-backed agentic comparison path.

Rule of thumb:

> manual from-scratch path remains primary; Quarkus AI integrations are additive comparison seams.

If a new class could be mistaken for the main path, its name should make the seam explicit with words like `Companion`, `Framework`, or `LangChain4j`.

LangChain4j AI services can participate in Quarkus observability around the service and tool boundaries, while the manual loop keeps its explicit Micrometer and OpenTelemetry instrumentation.
