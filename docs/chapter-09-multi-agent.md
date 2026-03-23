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
- The reviewer can reject thin outputs.
- Specialists stay small and focused.

## Demo vs Production

- Demo: static specialist responses and a lightweight reviewer.
- Production placeholder: richer routing heuristics and tool-using specialists.
