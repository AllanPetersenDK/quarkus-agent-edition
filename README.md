# Build an AI Agent from Scratch - Quarkus Edition

This repository is a Quarkus and Java 21 companion edition of *Build an AI Agent from Scratch*.
It is not the original Python book code. Instead, it is mapped from the official Python reference
zip and translated into a chapter-based, runnable Java application that is suitable as a public
reference implementation.

## Why Quarkus Edition?

- Java 21 gives us strong typing, records, and a mature enterprise toolchain.
- Quarkus keeps startup fast and the application model compact.
- The Python zip structure maps naturally onto CDI beans, REST endpoints, and chapter demos.
- The repo is built to be understandable, testable, and easy to extend.

## Project Structure

- `src/main/java/dk/ashlan/agent/chapters/chapter02` Java demos mapped from `chapter_02_llm/`.
- `src/main/java/dk/ashlan/agent/chapters/chapter03` Java demos mapped from `chapter_03_tool_use/`.
- `src/main/java/dk/ashlan/agent/chapters/chapter04` Java demos mapped from `chapter_04_basic_agent/`.
- `src/main/java/dk/ashlan/agent/chapters/chapter05` Java demos and RAG companion flows.
- `src/main/java/dk/ashlan/agent/chapters/chapter06` Java demos mapped from `chapter_06_memory/`.
- `src/main/java/dk/ashlan/agent/chapters/chapter07` Planning and reflection companion demos.
- `src/main/java/dk/ashlan/agent/chapters/chapter08` Code-agent companion demos.
- `src/main/java/dk/ashlan/agent/chapters/chapter09` Multi-agent companion demos.
- `src/main/java/dk/ashlan/agent/chapters/chapter10` Evaluation and monitoring companion demos.
- `src/main/java/dk/ashlan/agent/core` Agent loop, execution context, and orchestration.
- `src/main/java/dk/ashlan/agent/llm` LLM abstractions and model layer.
- `src/main/java/dk/ashlan/agent/tools` Generic tool contracts, registry, and execution.
- `src/main/java/dk/ashlan/agent/memory` Memory strategies and memory services.
- `src/main/java/dk/ashlan/agent/sessions` Session and cross-session abstractions.
- `src/main/java/dk/ashlan/agent/types` Shared content/event types.
- `src/main/java/dk/ashlan/agent/rag` Retrieval and knowledge-base support.
- `src/main/java/dk/ashlan/agent/planning` Planning and reflection services.
- `src/main/java/dk/ashlan/agent/code` Workspace-safe code-agent helpers.
- `src/main/java/dk/ashlan/agent/multiagent` Multi-agent coordination layer.
- `src/main/java/dk/ashlan/agent/eval` Evaluation and trace collection.
- `docs/` Architecture notes and chapter-by-chapter companion documentation.

## Python-to-Quarkus Mapping

See [`docs/python-to-quarkus-mapping.md`](docs/python-to-quarkus-mapping.md) for the file-by-file
mapping from the Python reference zip to the Quarkus edition.

## Module Structure

- `docs/` Architecture notes, mapping docs, and chapter notes.
- `src/main/resources/prompts/` Prompt templates for future provider integrations.
- `src/main/java/dk/ashlan/agent/chapters/*` Chapter demo classes mapped from the Python zip.
- `src/main/java/dk/ashlan/agent/*` Shared framework and Quarkus companion extensions.

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
- `WorkspaceService` defaults to `target/workspace` for safe local runs.

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

## Companion Extensions Beyond the Python Zip

- `src/main/java/dk/ashlan/agent/rag`
- `src/main/java/dk/ashlan/agent/planning`
- `src/main/java/dk/ashlan/agent/code`
- `src/main/java/dk/ashlan/agent/multiagent`
- `src/main/java/dk/ashlan/agent/eval`
