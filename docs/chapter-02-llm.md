# Chapter 02 - LLM

This chapter maps the Python `chapter_02_llm/` scripts and the shared `scratch_agents/models/` layer into Quarkus.

## Python Files

- `01_llm_chat.py`
- `02_conversation_management.py`
- `03_structured_output.py`
- `04_asynchronous_llm_call.py`
- `05_potato_problem.py`

## Quarkus Classes

- `dk.ashlan.agent.llm.LlmRequest`
- `dk.ashlan.agent.llm.LlmResponse`
- `dk.ashlan.agent.llm.LlmClient`
- `dk.ashlan.agent.llm.DemoToolCallingLlmClient`
- `dk.ashlan.agent.llm.OpenAiLlmClient`
- `dk.ashlan.agent.llm.StructuredOutputParser`
- `dk.ashlan.agent.core.LlmRequestBuilder`
- `dk.ashlan.agent.chapters.chapter02.*`

## Design Notes

- The Python request/response modeling becomes explicit Java records.
- The chapter demos use `Chapter02Support` to assemble deterministic in-memory flows.
- The async example is a small `CompletableFuture`-based demo rather than a direct coroutine port.
- The “potato problem” is represented as a validation/retry style demo instead of a Python-specific pattern.

## Demo vs Production

- Demo: deterministic message handling, simple parsing, and async placeholder calls.
- Production placeholder: `OpenAiLlmClient`.
