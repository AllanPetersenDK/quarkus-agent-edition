# Build an AI Agent from Scratch - Quarkus Edition

This repository is a Quarkus and Java 21 companion edition of *Build an AI Agent from Scratch*.
It is not the original Python book code. Instead, it translates the learning journey into a
chapter-based, runnable Java application that is suitable as a public reference implementation.

## Why Quarkus Edition?

- Java 21 gives us strong typing, records, and a mature enterprise toolchain.
- Quarkus keeps startup fast and the application model compact.
- The chapter structure maps naturally onto CDI beans and REST endpoints.
- The repo is built to be understandable, testable, and easy to extend.

## Project Structure

- `src/main/java/dk/ashlan/agent/api` REST endpoints for the chapter demos.
- `src/main/java/dk/ashlan/agent/core` Agent loop, execution context, and orchestration.
- `src/main/java/dk/ashlan/agent/llm` LLM abstractions and demo provider clients.
- `src/main/java/dk/ashlan/agent/tools` Generic tool contracts, registry, and execution.
- `src/main/java/dk/ashlan/agent/rag` Retrieval and knowledge-base support.
- `src/main/java/dk/ashlan/agent/memory` Session and long-term memory support.
- `src/main/java/dk/ashlan/agent/planning` Planning and reflection services.
- `src/main/java/dk/ashlan/agent/code` Workspace-safe code-agent helpers.
- `src/main/java/dk/ashlan/agent/multiagent` Multi-agent coordination layer.
- `src/main/java/dk/ashlan/agent/eval` Evaluation and trace collection.
- `docs/` Architecture notes and chapter-by-chapter companion documentation.

## Chapter Mapping

- Chapter 1: Agent vs workflow
- Chapter 2: LLM integration
- Chapter 3: Tools
- Chapter 4: Agent loop
- Chapter 5: RAG
- Chapter 6: Memory
- Chapter 7: Planning and reflection
- Chapter 8: Code agents
- Chapter 9: Multi-agent systems
- Chapter 10: Evaluation and monitoring

## Phase Overview

- Phase 1: Baseline Quarkus setup and repository hygiene
- Phase 2: Chapters 1-4 core agent foundations
- Phase 3: Chapter 5 retrieval-augmented generation
- Phase 4: Chapter 6 memory
- Phase 5: Chapter 7 planning and reflection
- Phase 6: Chapter 8 code agents
- Phase 7: Chapter 9 multi-agent coordination
- Phase 8: Chapter 10 evaluation and monitoring
- Phase 9: Documentation and cleanup

## Run Locally

```bash
mvn quarkus:dev
```

The application listens on `http://localhost:8080`.

## Run Tests

```bash
mvn test
```

## Current Status

The repository currently contains a working Quarkus companion implementation with deterministic demo
components for the learning chapters. It compiles and the test suite is green in the current setup.

Demo and fake components are intentionally marked and include:

- `DemoToolCallingLlmClient`
- `FakeEmbeddingClient`
- `InMemoryVectorStore`
- `InMemoryTaskMemoryStore`
- `CodeGenerationTool`
- `TestExecutionTool`

## Production Hardening Ideas

- Real LLM provider integration
- PostgreSQL with `pgvector`
- Redis-backed session and memory storage
- Micrometer metrics and OpenTelemetry tracing
- Authentication and authorization
- Durable persistence for traces, memory, and evaluation results

## Known Limitations

- The OpenAI client is a placeholder, not a real provider integration.
- The RAG, memory, and evaluation layers are in-memory demo implementations.
- Code generation and command execution are intentionally conservative placeholders.
- The multi-agent router is deterministic and intentionally simple.
