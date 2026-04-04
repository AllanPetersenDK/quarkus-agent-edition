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
- `src/main/java/dk/ashlan/agent/chapters/chapter07/companion` LangChain4j agentic comparison demo.
- `src/main/java/dk/ashlan/agent/chapters/chapter08` Code-agent companion demos.
- `src/main/java/dk/ashlan/agent/chapters/chapter09` Multi-agent companion demos.
- `src/main/java/dk/ashlan/agent/chapters/chapter10` Evaluation and monitoring companion demos.
- `src/main/java/dk/ashlan/agent/core` Agent loop, execution context, and orchestration.
- `src/main/java/dk/ashlan/agent/llm` LLM abstractions and model layer.
- `src/main/java/dk/ashlan/agent/mcp` MCP companion tools exposed on top of existing tool implementations.
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

## Mode Model

The repo uses three explicit modes so the companion story stays honest:

| Mode | Meaning | Examples |
|---|---|---|
| Demo | Deterministic chapter walkthroughs and local stand-ins | `DemoToolCallingLlmClient`, chapter 03/04/05/06 demo helpers, chapter 08 placeholder code execution |
| Runtime default | The normal CDI-backed runtime path in this companion app | `OpenAiLlmClient` when configured, H2-backed session state, H2-backed RAG chunks |
| Companion seam | Optional framework-backed comparison path | `LangChain4jLlmClient`, `LangChain4jToolCallingCompanionAssistant`, `CompanionMcpTools`, `LangChain4jAgenticCompanionDemo` |
| Production seam | Real external integration points that are intentionally isolated | OpenAI provider transport, external search, sandboxed code execution, auth, durable storage |

## Python-to-Quarkus Mapping

See [`docs/python-to-quarkus-mapping.md`](docs/python-to-quarkus-mapping.md) for the file-by-file
mapping from the Python reference zip to the Quarkus edition.
See [`docs/companion-seams.md`](docs/companion-seams.md) for the rule that keeps the manual path primary.

## Module Structure

- `docs/` Architecture notes, mapping docs, and chapter notes.
- `src/main/resources/prompts/` Prompt templates for future provider integrations.
- `src/main/java/dk/ashlan/agent/chapters/*` Chapter demo classes mapped from the Python zip.
- `src/main/java/dk/ashlan/agent/*` Shared framework and Quarkus companion extensions.

## Run Locally

```bash
mvn quarkus:dev
```

The application listens on `http://localhost:8080` and the helper script binds dev mode to `0.0.0.0` so WSL and browser access are both usable.

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
- `POST /api/agent/run`
- `GET /api/agent/tools`
- `GET /api/runtime/health`
- `GET /api/runtime/health/ready`
- `GET /api/runtime/health/live`
- `GET /api/runtime/sessions/{sessionId}`
- `GET /api/runtime/sessions/{sessionId}/memory`
- `POST /api/rag/ingest`
- `POST /api/rag/ingest/path`
- `GET /api/rag/query`
- `POST /admin/evaluations`
- `POST /admin/evaluations/gaia/run`
- `GET /admin/evaluations/gaia/{taskId}`
- `GET /admin/evaluations/gaia/runs/{runId}`
- `GET /admin/evaluations/{caseId}`
- `POST /api/companion/langchain4j/run`
- `POST /api/companion/langchain4j/agentic-demo`
- `POST /api/companion/llm/completions`
- `POST /api/companion/llm/async-batch`

For local GAIA validation, `./scripts/run-dev.sh` will auto-download the workspace snapshot into `target/gaia-data` on first start when `GAIA_DATASET_URL` is not set.
Chapter-5 path ingest now works the same way through the shared workspace/document read layer, so you can point the RAG companion seam at a workspace file path instead of copying text into a raw ingest request.
- `POST /code-agent`
- `POST /multi-agent`
- `GET /workflow-demo`
- MCP server: `http://localhost:8080/mcp`

See [`docs/api.md`](docs/api.md) for the Swagger coverage boundary and the exact split between REST-exposed outer seams and internal chapter mechanics.
See [`docs/fault-tolerance.md`](docs/fault-tolerance.md) for the current resilience policy on provider calls.
See [`docs/persistence.md`](docs/persistence.md) for the first H2-backed persistence layer.
See [`docs/security.md`](docs/security.md) for the current security stance on the public and admin seams.
See [`docs/storage-roadmap.md`](docs/storage-roadmap.md) for the next storage candidates after H2.

Tracing is prepared with OpenTelemetry spans around agent runs and tool execution, while OTLP export stays disabled by default so the repo does not depend on a collector during normal dev.

`OpenAiLlmClient` now uses Quarkus REST Client under the hood for OpenAI Chat Completions. The transport
is still isolated, tool-call round-tripping preserves provider `tool_call_id` metadata, and the
provider path is guarded with selective timeout plus retry via SmallRye Fault Tolerance.
The LangChain4j companion seams are opt-in comparison paths, and the tool-calling companion seam
exposes only calculator and clock through the existing repo tools so the internal tool model stays
the main learning path.

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
default unless `openai.api-key` is configured. A LangChain4j-backed companion client, a LangChain4j
tool-calling companion seam, and a tiny MCP server seam are also present, but they are optional
comparison paths rather than the main model.
Session state is now persisted to file-based H2 through `SessionManager`. The main RAG runtime path
also persists chunk text, metadata, and embeddings to H2 through `JdbcVectorStore`, while the chapter
demos still use explicit in-memory stores so the learning flow stays visible.
Quarkus CDI resolves the JDBC-backed persistence beans in runtime; the in-memory stores are the
explicit default/manual paths.
`AgentOrchestrator` also has a small callback seam now, with `after_run` acting as the bridge into
chapter-6 memory so the core loop stays simple while memory/persistence hooks remain extensible.

To enable the real OpenAI integrations locally, set the standard environment variable:

```bash
export OPENAI_API_KEY="your_api_key_here"
```

The LangChain4j companion seam also reads `OPENAI_API_KEY`. Secrets must never be committed; keep
them in local environment variables or your untracked `.env` file.

The manual loop still owns the explicit Micrometer counters and OpenTelemetry spans. Quarkus
LangChain4j AI services can add framework-managed observability around the companion service and its
tool invocations, which makes them useful for comparing manual and framework-backed instrumentation.

Demo and fake components are intentionally marked and include:

- `DemoToolCallingLlmClient`
- `OpenAiLlmClient` when `openai.api-key` is configured
- `LangChain4jLlmClient` when `agent.llm-provider=langchain4j`
- `LangChain4jToolCallingCompanionAssistant` when `OPENAI_API_KEY` is configured
- `FakeEmbeddingClient`
- `InMemoryVectorStore`
- `JdbcVectorStore` in the runtime CDI path
- `InMemoryTaskMemoryStore`
- `InMemorySessionStateStore` as the explicit fallback path
- `WebSearchTool` and `WikipediaTool` as lightweight local placeholders
- `inspect_path`, `unzip_file`, `list_files`, `read_file`, and `read_document_file` as chapter-5-style filesystem exploration tools under the same `ToolRegistry`/`ToolExecutor` path, with `read_media_file` kept as a compatibility alias. Filesystem access is read-only and stays inside the shared workspace root; symlink access is rejected.
- `CompanionMcpTools` as the MCP-facing comparison seam
- `CodeGenerationTool`
- `TestExecutionTool`
- `WorkspaceService` and the filesystem tools share the canonical `code.workspace-root`, which defaults to `target/workspace` for safe local runs.
- GAIA validation/dev defaults to `target/gaia-data`, so a downloaded GAIA snapshot can live inside the workspace without any machine-specific absolute path.
- Micrometer timers/counters are enabled for agent runs and tool execution.
- SmallRye Fault Tolerance backs the OpenAI retry policy, and the provider call is wrapped in a local timeout boundary.

## Swagger Coverage

Swagger documents the outer runtime and companion seams, not the internal learning mechanics.

Covered in Swagger:

- manual agent runs
- tool discovery
- runtime health, readiness, and liveness
- RAG query and document ingest
- session and memory inspection
- evaluation run and trace lookup
- GAIA validation/dev run and per-task lookup
- the selected LangChain4j companion run and agentic demo
- internal chapter demos for code-agent, multi-agent, and workflow

## GAIA Local Setup

For local validation/dev, GAIA defaults to a workspace-local snapshot under `target/gaia-data`.
That lets you keep the real Hugging Face snapshot in the repo workspace without committing dataset files.

Typical local flow:

```bash
./scripts/run-dev.sh
```

`scripts/run-dev.sh` automatically downloads the GAIA validation snapshot into `target/gaia-data` on first use when `GAIA_DATASET_URL` is not set.
If you want to prepare the snapshot without starting dev mode, run `scripts/setup-gaia-local.sh` directly.
Chapter-5 file exploration uses the same canonical workspace root and document-reading foundation as the code workspace tools, so the agent can step through zip files, plain text, PDFs, and supported audio without a separate extraction stack.
GAIA also uses the live `web-search` runtime tool for current web/video lookup questions, and common audio attachments such as `mp3`, `wav`, and `m4a` are transcribed when `OPENAI_API_KEY` is configured.
Set `GAIA_AUDIO_TRANSCRIPTION_MODEL` if you want to override the default `gpt-4o-mini-transcribe` audio model.
GAIA now also extracts plain text from text-like attachments and PDFs (`txt`, `md`, `csv`, `json`, `html`, `xml`, `pdf`) and injects the extracted text into the validation context. Unsupported types stay explicit, and OCR/vision are still out of scope.
For short entity-style answers, GAIA scoring is now stricter about long compound responses that mention the expected entity together with competing alternatives.
GAIA validation also applies a single-entity answer policy and conservative post-processing for questions that clearly expect one named entity, so the runner prefers one precise answer over a broad list when that is safe.

Example GAIA run body:

```json
{
  "localPath": "target/gaia-data",
  "config": "2023",
  "split": "validation",
  "level": 1,
  "limit": 3,
  "failFast": false
}
```

Still internal:

- `AgentOrchestrator` loop internals
- `LlmClientSelector`
- prompt and request builders
- manual `ToolRegistry` / `ToolExecutor` wiring
- `JdbcVectorStore` implementation details
- chapter helper classes that are only there to teach the learning flow
- the MCP server at `/mcp`

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
- Chapter demos still use in-memory RAG and memory helpers where that keeps the book mapping easier to follow.
- H2 is the first persistence step, not the final production datastore.
- Retrieval still uses simple cosine similarity over persisted rows rather than a dedicated vector index.
- `code-agent`, `multi-agent`, `admin/evaluations`, GAIA validation, runtime inspection, and RAG seams are production-style endpoints and are not protected by auth yet.
- Code generation and command execution are intentionally conservative placeholders.
- The multi-agent router is deterministic and intentionally simple.

## Companion Extensions Beyond the Python Zip

- `src/main/java/dk/ashlan/agent/mcp`
- `src/main/java/dk/ashlan/agent/chapters/chapter07/companion`
- `src/main/java/dk/ashlan/agent/rag`
- `src/main/java/dk/ashlan/agent/planning`
- `src/main/java/dk/ashlan/agent/code`
- `src/main/java/dk/ashlan/agent/multiagent`
- `src/main/java/dk/ashlan/agent/eval`
