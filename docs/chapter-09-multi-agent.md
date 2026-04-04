# Chapter 9 - Multi-Agent

## Chapter Goal

Coordinate specialist agents and add a reviewer step.

## Quarkus Translation

The multi-agent design is expressed as CDI-managed specialists, a router, and a coordinator.

## Central Classes

- `dk.ashlan.agent.multiagent.SpecialistAgent`
- `dk.ashlan.agent.multiagent.ResearchAgent`
- `dk.ashlan.agent.multiagent.CodingAgent`
- `dk.ashlan.agent.multiagent.ReviewerAgent`
- `dk.ashlan.agent.multiagent.AgentRouter`
- `dk.ashlan.agent.multiagent.CoordinatorAgent`
- `dk.ashlan.agent.chapters.chapter09.Chapter09Support`
- `dk.ashlan.agent.chapters.chapter09.ResearchCoordinationDemo`
- `dk.ashlan.agent.chapters.chapter09.CodingCoordinationDemo`
- `dk.ashlan.agent.chapters.chapter09.ReviewerDemo`

## Design Choices

- Routing is deterministic enough for tests and demos.
- The reviewer can reject thin outputs, so approval is a real signal rather than a decorative field.
- Specialists stay small and focused.
- The HTTP response exposes the selected specialist, route reason, reviewer outcome, and a compact coordinator summary so the chapter is observable without reading internals.
- Chapter 9 now also keeps a small run history, so multi-agent runs can be looked up again by `runId` through Swagger-visible history endpoints.

## Demo vs Production

- Demo: static specialist responses and a lightweight reviewer.
- Runtime default: deterministic routing and review in the companion app.
- Production placeholder: richer routing heuristics, tool-using specialists, and endpoint-level auth if exposed externally.
