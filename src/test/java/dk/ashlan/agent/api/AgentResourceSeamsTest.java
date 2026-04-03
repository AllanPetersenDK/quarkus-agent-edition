package dk.ashlan.agent.api;

import dk.ashlan.agent.api.dto.AgentRunRequest;
import dk.ashlan.agent.api.dto.AgentStepResponse;
import dk.ashlan.agent.api.dto.AgentStructuredRunRequest;
import dk.ashlan.agent.api.dto.AgentStructuredRunResponse;
import dk.ashlan.agent.core.AgentOrchestrator;
import dk.ashlan.agent.core.AgentStepResult;
import dk.ashlan.agent.core.AgentTraceEntry;
import dk.ashlan.agent.llm.LlmToolCall;
import dk.ashlan.agent.tools.JsonToolResult;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.ws.rs.BadRequestException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentResourceSeamsTest {
    @Test
    void stepRequestIsValidatedAndStepResponseIsStructured() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        assertFalse(validator.validate(new AgentRunRequest(" ", null)).isEmpty());

        AgentOrchestrator orchestrator = new AgentOrchestrator(null, null, null, null, 1, "") {
            @Override
            public AgentStepResult step(String message, String sessionId) {
                return new AgentStepResult(
                        sessionId,
                        3,
                        null,
                        List.of(new LlmToolCall("calculator", Map.of("expression", "25 * 4"), "call-123")),
                        List.of(JsonToolResult.success("calculator", "100")),
                        null,
                        false,
                        List.of(
                                new AgentTraceEntry("step", "iteration:3"),
                                new AgentTraceEntry("tool-call", "calculator"),
                                new AgentTraceEntry("tool-result", "100")
                        )
                );
            }
        };
        AgentResource resource = new AgentResource(orchestrator);

        AgentStepResponse response = resource.step(new AgentRunRequest("What is 25 * 4?", "demo-session"));

        assertEquals("demo-session", response.sessionId());
        assertEquals(3, response.stepNumber());
        assertEquals(1, response.toolCalls().size());
        assertEquals("calculator", response.toolCalls().get(0).toolName());
        assertEquals("100", response.toolResults().get(0).output());
        assertFalse(response.isFinal());
        assertNotNull(response.traceEntries());
    }

    @Test
    void structuredRunReturnsValidationStatusAndRejectsUnsupportedModes() {
        AgentOrchestrator orchestrator = new AgentOrchestrator(null, null, null, null, 1, "") {
            @Override
            public AgentStepResult step(String message, String sessionId) {
                return new AgentStepResult(
                        sessionId,
                        1,
                        "answer: structured demo result",
                        List.of(),
                        List.of(),
                        "answer: structured demo result",
                        true,
                        List.of(
                                new AgentTraceEntry("step", "iteration:1"),
                                new AgentTraceEntry("assistant-message", "answer: structured demo result")
                        )
                );
            }
        };
        AgentResource resource = new AgentResource(orchestrator);

        AgentStructuredRunResponse response = resource.runStructured(
                new AgentStructuredRunRequest("Return a structured answer.", "structured-session", "chapter4-answer")
        );

        assertEquals("structured-session", response.sessionId());
        assertEquals("chapter4-answer", response.mode());
        assertEquals(AgentStructuredRunResponse.StructuredOutputValidationStatus.VALIDATED, response.validationStatus());
        assertNotNull(response.structuredResult());
        assertEquals("structured demo result", response.structuredResult().answer());
        assertEquals("FINAL_ANSWER", response.stopReason().name());
        assertTrue(response.step().isFinal());

        BadRequestException exception = assertThrows(BadRequestException.class, () -> resource.runStructured(
                new AgentStructuredRunRequest("Return a structured answer.", "structured-session", "banana")
        ));
        assertTrue(exception.getMessage().contains("Unsupported structured-output mode"));
    }
}
