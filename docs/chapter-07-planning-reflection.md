# Chapter 7 - Planning and Reflection

## Chapter Goal

Plan before execution and reflect after execution so the agent can improve thin answers.

## Quarkus Translation

The Java edition keeps planning and reflection as explicit services and small runtime tools around the main agent runner. This stays companion/runtime-grade: there is no new workflow engine, only a lightweight task-plan and reflection cycle on top of the existing manual agent core.

## Central Classes

- `dk.ashlan.agent.planning.ExecutionPlan`
- `dk.ashlan.agent.planning.PlanStep`
- `dk.ashlan.agent.planning.PlannerService`
- `dk.ashlan.agent.planning.ReflectionService`
- `dk.ashlan.agent.planning.PlannedAgentOrchestrator`
- `dk.ashlan.agent.planning.CreateTasksTool`
- `dk.ashlan.agent.planning.ReflectionTool`
- `dk.ashlan.agent.planning.TaskItem`
- `dk.ashlan.agent.planning.TaskStatus`
- `dk.ashlan.agent.chapters.chapter07.Chapter07Support`
- `dk.ashlan.agent.chapters.chapter07.PlanningDemo`
- `dk.ashlan.agent.chapters.chapter07.ReflectionDemo`
- `dk.ashlan.agent.chapters.chapter07.ImprovementLoopDemo`
- `dk.ashlan.agent.chapters.chapter07.companion.CompanionPlanWorkflow`
- `dk.ashlan.agent.chapters.chapter07.companion.LangChain4jAgenticCompanionDemo`
- `dk.ashlan.agent.chapters.chapter07.companion.LangChain4jToolCallingCompanionDemo`

## Design Choices

- Planning happens before execution.
- Reflection can reject answers that are too thin and can mark a replan need.
- The re-entry loop is deterministic in the companion edition so it is easy to test.
- The manual planning/reflection loop remains the baseline learning path, while the new tools let the same idea show up in the runtime registry and the existing `/api/agent/run` seam.
- The LangChain4j agentic demo is a comparison seam that shows a framework-backed workflow without replacing the manual services.
- The LangChain4j tool-calling companion demo is a separate comparison seam that exercises the framework-backed tool path without replacing the manual services.

## Demo vs Production

- Demo: simple planning and reflection with the task-plan tool and the review tool.
- Production placeholder: LLM-generated plans and reviewer-style grading.
- Companion demo: `LangChain4jAgenticCompanionDemo` for the framework-backed comparison path.
- Companion tool-calling demo: `LangChain4jToolCallingCompanionDemo` for the framework-backed tool-calling comparison path.
