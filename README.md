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

If your local Maven is older than 3.9, use the helper script instead:

```bash
bash scripts/run-dev.sh
```

To run a specific chapter smoke test with one command, use one of these:

```bash
bash scripts/run-chapter-02.sh
bash scripts/run-chapter-03.sh
bash scripts/run-chapter-04.sh
bash scripts/run-chapter-05.sh
bash scripts/run-chapter-06.sh
bash scripts/run-chapter-07.sh
bash scripts/run-chapter-08.sh
bash scripts/run-chapter-09.sh
bash scripts/run-chapter-10.sh
```

## Run Tests

```bash
mvn test
```

## API

- OpenAPI: `http://localhost:8080/openapi`
- Swagger UI: `http://localhost:8080/swagger-ui`
- Health: `http://localhost:8080/q/health`
- Readiness: `http://localhost:8080/q/health/ready`
- Liveness: `http://localhost:8080/q/health/live`
- `POST /api/agent/run`
- `GET /api/agent/tools`

See [`docs/api.md`](docs/api.md) for request/response examples, Quarkus OpenAPI properties, and the note on deferred session and memory endpoints.
See [`docs/fault-tolerance.md`](docs/fault-tolerance.md) for the current resilience policy on provider calls.
See [`docs/persistence.md`](docs/persistence.md) for the first H2-backed persistence layer.

Tracing is prepared with OpenTelemetry spans around agent runs and tool execution, while OTLP export stays disabled by default so the repo does not depend on a collector during normal dev.

`OpenAiLlmClient` now uses Quarkus REST Client under the hood for OpenAI Chat Completions. The transport
is still isolated, tool-call round-tripping preserves provider `tool_call_id` metadata, and the
provider path is guarded with selective timeout plus retry via SmallRye Fault Tolerance.

## Build the Companion PDF

```bash
python3 scripts/build_companion_pdf.py
```

The generated file is written to `target/quarkus-agent-edition-companion.pdf`.
The build uses the local book materials in `docs/book/`, including the MEAP PDF and the official
Python source zip from `https://github.com/shangrilar/ai-agent-from-scratch`. The PDF generator
extracts book snippets and rewrites them with Quarkus-focused chapter content.

## Current Status

The repository currently contains a working Quarkus companion implementation with deterministic demo
components for the learning chapters. It compiles and the test suite is green in the current setup.
`OpenAiLlmClient` is now a real HTTP integration seam for OpenAI Chat Completions, and tool-call
round-tripping keeps provider `tool_call_id` metadata intact. The demo client still remains the
default unless `openai.api-key` is configured.
Session state is now persisted to file-based H2 through `SessionManager`, while RAG chunk storage
and the remaining demo stores stay in-memory for now.

To enable it locally, set an API key via config or environment variable:

```bash
export OPENAI_API_KEY=your-key-here
```

Demo and fake components are intentionally marked and include:

- `DemoToolCallingLlmClient`
- `OpenAiLlmClient` when `openai.api-key` is configured
- `FakeEmbeddingClient`
- `InMemoryVectorStore`
- `InMemoryTaskMemoryStore`
- `CodeGenerationTool`
- `TestExecutionTool`
- `WorkspaceService` defaults to `target/workspace` for safe local runs.
- Micrometer timers/counters are enabled for agent runs and tool execution.
- SmallRye Fault Tolerance backs the OpenAI retry policy, and the provider call is wrapped in a local timeout boundary.

## Production Hardening Ideas

- Real LLM provider integration
- PostgreSQL with `pgvector`
- Redis-backed memory or session storage
- Micrometer metrics and OpenTelemetry tracing
- REST Client-backed provider calls
- timeout boundary and retry on provider calls
- Authentication and authorization
- Durable persistence for traces, memory, and evaluation results

## Known Limitations

- The OpenAI integration is real, but it only activates when `openai.api-key` is configured.
- RAG and evaluation layers are still in-memory demo implementations, while session memory is now persisted in H2.
- Code generation and command execution are intentionally conservative placeholders.
- The multi-agent router is deterministic and intentionally simple.

## Companion Extensions Beyond the Python Zip

- `src/main/java/dk/ashlan/agent/rag`
- `src/main/java/dk/ashlan/agent/planning`
- `src/main/java/dk/ashlan/agent/code`
- `src/main/java/dk/ashlan/agent/multiagent`
- `src/main/java/dk/ashlan/agent/eval`
