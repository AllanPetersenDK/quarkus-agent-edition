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
- `quarkus.smallrye-openapi.info-description=Quarkus companion edition of Build an AI Agent from Scratch. Swagger documents the HTTP-exposed outer seams: manual agent runs, tool discovery, runtime health, RAG query and ingest, session and memory inspection, evaluation run and trace lookup, and the selected LangChain4j companion demos. Manual orchestration internals, selector logic, prompt builders, and low-level storage details remain Java-only unless a specific endpoint exposes them.`

## Swagger Coverage

Swagger now documents the outer runtime and companion seams that are practical to exercise over HTTP.

Covered in Swagger:

- `POST /api/agent/run` - manual runtime agent loop
- `GET /api/agent/tools` - runtime tool discovery
- `GET /api/runtime/health` - combined readiness and liveness view
- `GET /api/runtime/health/ready` - readiness snapshot
- `GET /api/runtime/health/live` - liveness snapshot
- `GET /api/runtime/sessions/{sessionId}` - session inspection
- `GET /api/runtime/sessions/{sessionId}/memory` - memory inspection
- `POST /api/rag/ingest` - document ingest into the RAG stack
- `GET /api/rag/query` - RAG query and answer
- `POST /admin/evaluations` - evaluation run
- `GET /admin/evaluations/{caseId}` - evaluation trace lookup
- `POST /api/companion/langchain4j/run` - LangChain4j companion run
- `POST /api/companion/langchain4j/agentic-demo` - LangChain4j agentic companion demo
- `POST /api/companion/llm/completions` - chapter-02 companion direct chat simulation
- `POST /api/companion/llm/async-batch` - chapter-02 companion async batch demo
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

### Tool Discovery

`GET /api/agent/tools`

Utility/discovery endpoint: this lists the runtime tool registry and does not execute tools.

### Runtime Inspection

`GET /api/runtime/health`

Read-only runtime inspection seam that shows readiness and liveness together.

`GET /api/runtime/sessions/{sessionId}`

Read-only session inspection seam that exposes the stored conversation messages.

`GET /api/runtime/sessions/{sessionId}/memory`

Read-only memory inspection seam that returns the relevant memories for a session and query.

### RAG

`POST /api/rag/ingest`

Swagger-visible companion seam for document ingest into the repo's RAG stack.

`GET /api/rag/query`

Swagger-visible companion seam for querying the stored knowledge base.

### Evaluation

`POST /admin/evaluations`

Internal admin seam that runs chapter evaluation cases and returns results plus metrics.

`GET /admin/evaluations/{caseId}`

Read-only trace lookup for the most recent stored evaluation run with the given case id.

### LangChain4j Companion

`POST /api/companion/langchain4j/run`

Framework-backed companion comparison seam that answers a single prompt.

`POST /api/companion/langchain4j/agentic-demo`

Framework-backed agentic comparison seam that runs the chapter 07 planning workflow.

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
