# API

This repository exposes a Swagger-visible surface for selected outer runtime and companion seams.
It does not turn the from-scratch orchestration internals into HTTP endpoints.

## OpenAPI And Swagger UI

- OpenAPI JSON/YAML: `http://localhost:8080/openapi`
- Swagger UI: `http://localhost:8080/swagger-ui`

The OpenAPI and Swagger UI setup follows the official Quarkus `smallrye-openapi` approach.

Configured properties:

- `quarkus.smallrye-openapi.path=/openapi`
- `quarkus.swagger-ui.always-include=true`
- `quarkus.swagger-ui.path=/swagger-ui`
- `quarkus.smallrye-openapi.info-title=Quarkus Agent Edition API`
- `quarkus.smallrye-openapi.info-version=0.1.0`
- `quarkus.smallrye-openapi.info-description=Quarkus companion edition of Build an AI Agent from Scratch. Swagger documents the HTTP-exposed outer seams: manual agent runs, tool discovery, runtime health, RAG query and ingest, session and memory inspection, evaluation run, GAIA validation/dev run, trace lookup, and the selected LangChain4j companion demos. Manual orchestration internals, selector logic, prompt builders, and low-level storage details remain Java-only unless a specific endpoint exposes them.`

## Swagger Coverage

Swagger now documents the outer runtime and companion seams that are practical to exercise over HTTP.

Covered in Swagger:

- `POST /api/agent/run` - chapter-4 runtime/basic agent seam
- `POST /api/agent/step` - chapter-4 ReAct step seam
- `POST /api/agent/run/structured` - chapter-4 structured-output seam
- `GET /api/agent/tools` - chapter 3/4 boundary seam for runtime tool discovery
- `GET /api/runtime/health` - combined readiness and liveness view
- `GET /api/runtime/health/ready` - readiness snapshot
- `GET /api/runtime/health/live` - liveness snapshot
- `GET /api/runtime/sessions/{sessionId}` - session inspection, more naturally chapter 6-oriented than chapter 4-oriented
- `GET /api/runtime/sessions/{sessionId}/memory` - memory inspection, more naturally chapter 6-oriented than chapter 4-oriented
- `GET /api/runtime/sessions/{sessionId}/trace` - chapter-4 runtime trace inspection seam
- `POST /api/rag/ingest` - chapter 5-oriented document ingest into the RAG stack
- `POST /api/rag/ingest/path` - chapter 5-oriented workspace path ingest through the shared document-read layer
- `POST /api/rag/ingest/directory` - chapter 5-oriented bulk directory ingest through the shared document-read layer
- `GET /api/rag/query` - chapter 5-oriented RAG query and answer
- `POST /admin/evaluations` - evaluation run
- `POST /admin/evaluations/gaia/run` - GAIA validation/dev run with level filtering and attachment-aware context
- `GET /admin/evaluations/gaia/{taskId}` - GAIA task lookup
- `GET /admin/evaluations/gaia/runs/{runId}` - GAIA run lookup
- `GET /admin/evaluations/{caseId}` - evaluation trace lookup
- `POST /api/companion/langchain4j/run` - LangChain4j companion run
- `POST /api/companion/langchain4j/agentic-demo` - LangChain4j agentic companion demo
- `POST /api/companion/llm/completions` - chapter-02 companion direct chat simulation
- `POST /api/companion/llm/async-batch` - chapter-02 companion async batch demo with bounded concurrency and per-prompt failure isolation
- `POST /code-agent` - internal chapter demo for the deterministic code workflow
- `POST /multi-agent` - internal chapter demo for the coordinator/reviewer flow
- `GET /workflow-demo` - internal deterministic workflow demo

Not covered in Swagger:

- `AgentOrchestrator` loop internals
- `LlmClientSelector`
- request/prompt builders
- manual `ToolRegistry` and `ToolExecutor` wiring
- `JdbcVectorStore` implementation details
- internal chapter helper classes whose value is teaching flow rather than external invocation
- the built-in Quarkus health endpoints under `/q/health`
- the MCP server at `/mcp`

## Endpoint Notes

### Manual Runtime

`POST /api/agent/run`

Runtime API: this is the main REST-exposed manual agent loop.
Same-session calls now replay prior role-aware conversation history, so a session can remember user-provided facts across turns without relying on tool memory.

Request body:

```json
{
  "message": "What is 25 * 4?",
  "sessionId": "default"
}
```

Response body:

```json
{
  "answer": "25 * 4 = 100",
  "stopReason": "FINAL_ANSWER",
  "iterations": 1,
  "trace": [
    "iteration:1",
    "answer:25 * 4 = 100"
  ]
}
```

Field notes:

- `message` is required and validated with Jakarta Bean Validation.
- `sessionId` is optional and defaults to `default` when omitted or blank.
- `answer` maps directly from `AgentRunResult.finalAnswer()`.
- `stopReason` maps from the existing `StopReason` enum.
- `iterations` and `trace` map directly from the existing agent runtime result.

`POST /api/agent/step`

Chapter-4 ReAct step seam. This runs one manual think/act cycle and returns a structured view of that single step instead of the full loop.

Request body:

```json
{
  "message": "What is 25 * 4?",
  "sessionId": "default"
}
```

Response body:

```json
{
  "sessionId": "default",
  "stepNumber": 1,
  "assistantMessage": null,
  "toolCalls": [
    {
      "toolName": "calculator",
      "arguments": { "expression": "25 * 4" },
      "callId": "call-123"
    }
  ],
  "toolResults": [
    {
      "toolName": "calculator",
      "success": true,
      "output": "100",
      "data": { "output": "100" }
    }
  ],
  "finalAnswer": null,
  "isFinal": false,
  "traceEntries": [
    { "kind": "step", "message": "iteration:1" },
    { "kind": "tool-call", "message": "calculator" },
    { "kind": "tool-result", "message": "100" }
  ]
}
```

`POST /api/agent/run/structured`

Chapter-4 structured-output seam. This supports one controlled demo schema named `chapter4-answer` and returns a normalized structured answer plus the raw one-step agent result.

Request body:

```json
{
  "message": "Answer in a single sentence.",
  "sessionId": "default",
  "mode": "chapter4-answer"
}
```

Response body:

```json
{
  "sessionId": "default",
  "mode": "chapter4-answer",
  "validationStatus": "VALIDATED",
  "structuredResult": {
    "answer": "Direct answer: Answer in a single sentence."
  },
  "step": {
    "sessionId": "default",
    "stepNumber": 1,
    "assistantMessage": "Direct answer: Answer in a single sentence.",
    "toolCalls": [],
    "toolResults": [],
    "finalAnswer": "Direct answer: Answer in a single sentence.",
    "isFinal": true,
    "traceEntries": [
      { "kind": "step", "message": "iteration:1" },
      { "kind": "assistant-message", "message": "Direct answer: Answer in a single sentence." }
    ]
  },
  "stopReason": "FINAL_ANSWER"
}
```

### Tool Discovery

`GET /api/agent/tools`

Utility/discovery endpoint: this lists the runtime tool registry and does not execute tools.
The registry now includes chapter-5-style filesystem tools for controlled workspace exploration: `inspect_path`, `unzip_file`, `list_files`, `read_file`, and `read_document_file`. `read_media_file` remains available as a compatibility alias. All filesystem access is read-only, bound to the shared workspace root model used by the code workspace tools, and rejects symlink access.

### Runtime Inspection

`GET /api/runtime/health`

Read-only runtime inspection seam that shows readiness and liveness together.

`GET /api/runtime/sessions/{sessionId}`

Read-only session inspection seam that exposes the stored conversation messages.

`GET /api/runtime/sessions/{sessionId}/memory`

Read-only memory inspection seam that returns the relevant memories for a session and query.

`GET /api/runtime/sessions/{sessionId}/trace`

Read-only chapter-4 trace inspection seam that returns the structured step history recorded for the session.

### RAG

`POST /api/rag/ingest`

Swagger-visible companion seam for document ingest into the repo's RAG stack.

`POST /api/rag/ingest/path`

Swagger-visible chapter-5 companion seam for ingesting a workspace document by path. The endpoint resolves the path against the canonical workspace root, runs it through the shared document-read layer, and ingests the extracted text into RAG. Directory ingest is not supported in this first version; the response makes that explicit.
Structured failures are returned in the response body, including statuses such as `INVALID_PATH`, `SECURITY_VIOLATION`, `DIRECTORY_UNSUPPORTED`, `UNSUPPORTED_TYPE`, and `RESOLUTION_FAILED`, so clients can distinguish user input errors from successful ingest.

Example request body:

```json
{
  "path": "docs/chapter5/sample.pdf",
  "sourceId": "sample-pdf"
}
```

The shared document-read layer now covers text-like files such as `txt`, `md`, `csv`, `tsv`, `json`, `html`, `xml`, `properties`, `log`, `ini`, `rst`, `toml`, and common source-like text files, plus PDFs and supported audio through the same normalization seam used by GAIA and the filesystem tools.
It also covers office-style documents such as `docx`, `pptx`, `xlsx`, and `ipynb`, so path ingest can reuse the same extraction seam for more realistic chapter-5 documents.

`POST /api/rag/ingest/directory`

Swagger-visible chapter-5 companion seam for bulk ingesting a workspace directory. The endpoint resolves the directory against the canonical workspace root, reads each candidate through the shared document layer, and ingests only the documents that were actually read successfully. Unsupported files are reported explicitly in the response instead of stopping the whole batch.

Example request body:

```json
{
  "path": "docs/chapter5/samples",
  "sourceIdPrefix": "samples",
  "recursive": false,
  "maxFiles": 20
}
```

Per-file results report `INGESTED`, `SKIPPED_UNSUPPORTED`, `SKIPPED_DIRECTORY`, `READ_FAILED`, `SECURITY_VIOLATION`, `INVALID_PATH`, or `RESOLUTION_FAILED`, so bulk ingest stays easy to debug while remaining workspace-safe.

`GET /api/rag/query`

Swagger-visible companion seam for querying the stored knowledge base.
The chapter-5 query flow now uses a small hybrid reranker and builds `answer` from the best matching chunk instead of concatenating all retrieved chunks.
Query responses also include `bestChunk` and compact `citations` so clients can see the winning source and the ranked evidence behind the answer without parsing the full chunk list.

Example response:

```json
{
  "query": "Which text mentions PostgreSQL?",
  "answer": "Source docs/postgresql.txt mentions PostgreSQL: PostgreSQL is an open-source relational database.",
  "bestChunk": {
    "sourceId": "docs/postgresql.txt",
    "chunkIndex": 0,
    "text": "PostgreSQL is an open-source relational database.",
    "similarity": 0.92
  },
  "citations": [
    {
      "sourceId": "docs/postgresql.txt",
      "sourcePath": "docs/postgresql.txt",
      "chunkIndex": 0,
      "chunkId": "docs/postgresql.txt:0:abcd1234",
      "fileType": "txt",
      "contentType": "text/plain",
      "documentStatus": "TEXT_EXTRACTED",
      "sectionHint": "PostgreSQL is an open-source relational database.",
      "similarity": 0.92
    }
  ],
  "chunks": [
    {
      "sourceId": "docs/postgresql.txt",
      "chunkIndex": 0,
      "text": "PostgreSQL is an open-source relational database.",
      "metadata": {
        "sourceId": "docs/postgresql.txt",
        "sourcePath": "docs/postgresql.txt",
        "fileType": "txt",
        "contentType": "text/plain",
        "documentStatus": "TEXT_EXTRACTED",
        "chunkId": "docs/postgresql.txt:0:abcd1234"
      },
      "similarity": 0.92
    }
  ]
}
```

### Evaluation

`POST /admin/evaluations`

Internal admin seam that runs chapter evaluation cases and returns results plus metrics.

`POST /admin/evaluations/gaia/run`

GAIA validation/dev seam that loads a real GAIA validation snapshot from either a Hugging Face parquet URL or a local path. The flow resolves attachment presence into trace/context notes, extracts plain text from text-like attachments and PDFs, transcribes common audio attachments when OpenAI audio transcription is available, applies deterministic scoring, and runs the existing manual runtime agent on a selectable subset. Current web or video lookup questions can use the runtime `web-search` tool.
Short entity-style answers are scored more strictly now: long compound responses that mention the expected entity alongside competing alternatives do not pass as easily.
For GAIA validation, questions that clearly expect one named entity also get a small single-entity answer policy and a conservative post-processing step so the runner prefers one precise answer over a broad list when that is safe.
For local dev, the default workspace path is `target/gaia-data`, so a snapshot downloaded into the repo workspace can be used without any machine-specific absolute path.
The recommended local flow is simply `./scripts/run-dev.sh`; if the workspace snapshot is missing, the script downloads the Hugging Face validation tree into `target/gaia-data` first.

Example local request:

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

Example Hugging Face request:

```json
{
  "datasetUrl": "https://huggingface.co/datasets/gaia-benchmark/GAIA/resolve/main/2023/validation/metadata.level1.parquet",
  "level": 1,
  "limit": 3,
  "failFast": false
}
```

`GET /admin/evaluations/gaia/{taskId}`

Read-only lookup for the most recent GAIA case result stored for a given task id.

`GET /admin/evaluations/gaia/runs/{runId}`

Read-only lookup for a stored GAIA run result.

`GET /admin/evaluations/{caseId}`

Read-only trace lookup for the most recent stored evaluation run with the given case id.

### LangChain4j Companion

`POST /api/companion/langchain4j/run`

Framework-backed companion comparison seam that answers a single prompt.

`POST /api/companion/langchain4j/agentic-demo`

Framework-backed agentic comparison seam that runs the chapter 07 planning workflow.

### Chapter-02 Companion LLM

`POST /api/companion/llm/completions`

Book chapter: 2 companion/debug seam for direct chat-style requests through Swagger UI.

`POST /api/companion/llm/async-batch`

Book chapter: 2 companion/debug async batch seam for server-side concurrent direct LLM calls. The endpoint uses bounded concurrency, keeps results in input order, and returns per-prompt errors instead of failing the whole batch.

### Internal Chapter Demos

`POST /code-agent`

Internal chapter demo for the deterministic code workflow.

`POST /multi-agent`

Internal chapter demo for the coordinator/reviewer flow.

`GET /workflow-demo`

Internal deterministic workflow demo.

## Design Notes

- The public API is intentionally thin.
- The REST layer delegates to the existing runtime, RAG, memory, health, and evaluation beans.
- The implementation keeps the existing runtime behavior intact rather than introducing a second agent model.
- Internal chapter demos remain Java classes unless a specific endpoint exposes them as a comparison seam.

## Security Stance

- `POST /api/agent/run` is the main companion API and is safe for local/private use as-is.
- `GET /api/agent/tools` is harmless metadata and can remain open in the companion app.
- `POST /api/rag/ingest`, `GET /api/rag/query`, `POST /admin/evaluations`, and the runtime inspection endpoints should be treated as companion/admin seams until a fuller auth layer is added.
- If those seams need external exposure, add Quarkus OIDC and role checks instead of ad hoc request-time logic.
