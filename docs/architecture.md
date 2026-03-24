# Architecture Overview

This repository is a Quarkus companion edition of *Build an AI Agent from Scratch*.
The original Python ideas are translated into a Java 21 application with explicit package boundaries,
constructor injection, and chapter-aligned modules.

## Core Ideas

- `core` contains the agent loop, orchestration, execution context, and stop reasons.
- `llm` contains the LLM abstraction, a demo tool-calling client, and a Quarkus REST Client integration seam for OpenAI.
- `tools` provides generic tool definitions, registry, and execution.
- `rag`, `memory`, `planning`, `code`, `multiagent`, and `eval` each represent one chapter module.

## Design Choices

- Demo implementations are intentionally deterministic so they are easy to test.
- Production seams are explicit and only become active when configured, while demo components remain the default.
- In-memory stores are still used where the book concept matters more than infrastructure, but session state now has an H2-backed persistence seam and the main RAG runtime path persists chunk/embedding rows in H2.
- Path safety and injection boundaries are handled as first-class concerns in the Java version.
- Provider-backed tool calls preserve provider call IDs, so the agent loop can round-trip tool execution metadata without losing protocol fidelity.
- Micrometer records agent and tool runtime metrics, while OpenTelemetry still covers tracing spans.

## Mode Model

- Demo: deterministic chapter walkthroughs, placeholder code execution, and local stand-ins for search or embeddings.
- Runtime default: CDI-backed runtime with OpenAI transport only when configured, H2-backed session/RAG storage, and the normal agent flow.
- Production seam: external search, sandboxed execution, auth, durable storage, and any real provider-facing integration.

## Demo vs Production

- Demo: calculator, clock, fake embeddings, in-memory vector store in the chapter demos, H2-backed session state, code generation placeholder.
- Production seam: `OpenAiLlmClient`, H2-backed RAG persistence, code execution, and external search integrations.
- The structure is ready for PostgreSQL, Redis, metrics, tracing, and auth without changing the chapter map.
