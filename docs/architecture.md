# Architecture Overview

This repository is a Quarkus companion edition of *Build an AI Agent from Scratch*.
The original Python ideas are translated into a Java 21 application with explicit package boundaries,
constructor injection, and chapter-aligned modules.

## Core Ideas

- `core` contains the agent loop, orchestration, execution context, and stop reasons.
- `llm` contains the LLM abstraction, a demo tool-calling client, and a production placeholder.
- `tools` provides generic tool definitions, registry, and execution.
- `rag`, `memory`, `planning`, `code`, `multiagent`, and `eval` each represent one chapter module.

## Design Choices

- Demo implementations are intentionally deterministic so they are easy to test.
- Production placeholders are explicit and do not pretend to be real provider integrations.
- In-memory stores are used where the book concept matters more than infrastructure.
- Path safety and injection boundaries are handled as first-class concerns in the Java version.

## Demo vs Production

- Demo: calculator, clock, fake embeddings, in-memory vector store, in-memory memory store, code generation placeholder.
- Production placeholder: `OpenAiLlmClient`, code execution, and external search integrations.
- The structure is ready for PostgreSQL, Redis, metrics, tracing, and auth without changing the chapter map.
