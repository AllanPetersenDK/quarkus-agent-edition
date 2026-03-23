# Chapter 1 - Agent vs Workflow

## Chapter Goal

Show the difference between a deterministic workflow and an autonomous agent.

## Quarkus Translation

The Python-first idea becomes two REST-facing paths:

- `/workflow-demo` for a fixed, predictable workflow
- `/agent` for an agent loop that can call tools

## Central Classes

- `dk.ashlan.agent.api.WorkflowResource`
- `dk.ashlan.agent.api.AgentResource`
- `dk.ashlan.agent.core.AgentOrchestrator`
- `dk.ashlan.agent.core.ExecutionContext`

## Design Choices

- The workflow example is intentionally simple and deterministic.
- The agent path is the foundation for later chapters.
- REST resources stay thin and delegate to application services.

## Demo vs Production

- Demo: the workflow endpoint.
- Production-ready shape: the agent entrypoint and orchestration boundaries.
