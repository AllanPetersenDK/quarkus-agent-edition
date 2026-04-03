# API

This repository now exposes a small Quarkus REST surface backed by the existing agent runtime and tool registry.

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
- `quarkus.smallrye-openapi.info-description=OpenAPI for quarkus-agent-edition`

## Agent Endpoint

`POST /api/agent/run`

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

## Tools Endpoint

`GET /api/agent/tools`

Response body:

```json
[
  {
    "name": "calculator",
    "description": "Evaluate a simple arithmetic expression."
  },
  {
    "name": "clock",
    "description": "Return the current time in ISO-8601 format."
  }
]
```

The tool list is sourced from the existing `ToolRegistry` and uses each tool's `ToolDefinition` metadata.

## Design Notes

- The public API is intentionally thin.
- The REST layer delegates directly to `AgentOrchestrator` and `ToolRegistry`.
- The implementation keeps the existing runtime behavior intact rather than introducing a second agent model.
- A separate MCP server seam is exposed at `/mcp` for calculator and clock only, so the internal tool model remains the primary chapter 3 path.

## Deferred

Session and memory endpoints are intentionally deferred for now.

The current session/memory layer is useful internally, but it is not yet a stable public API surface, so exposing it now would create a half-finished contract.

## Security Stance

- `POST /api/agent/run` is the main companion API and is safe for local/private use as-is.
- `GET /api/agent/tools` is harmless metadata and can remain open in the companion app.
- `POST /code-agent`, `POST /multi-agent`, and `POST /admin/evaluations` are production seams and should be treated as internal-only until an auth layer is added.
- If those seams need external exposure, add Quarkus OIDC and role checks instead of ad hoc request-time logic.
