package dk.ashlan.agent.api;

import dk.ashlan.agent.chapters.chapter07.companion.LangChain4jAgenticCompanionDemo;
import dk.ashlan.agent.llm.LangChain4jCompanionAssistant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CompanionResourceTest {
    @Test
    void companionEndpointsExposeTheFrameworkSeams() {
        LangChain4jCompanionAssistant assistant = request -> "Framework answer for " + request;
        LangChain4jAgenticCompanionDemo demo = new LangChain4jAgenticCompanionDemo(topic -> "Plan for " + topic);
        CompanionResource resource = new CompanionResource(assistant, demo);

        CompanionResource.CompanionRunResponse run = resource.run(new CompanionResource.CompanionRunRequest("Hello"));
        CompanionResource.CompanionDemoResponse agentic = resource.agenticDemo(new CompanionResource.CompanionDemoRequest("RAG"));

        assertEquals("Framework answer for Hello", run.answer());
        assertEquals("LangChain4j companion seam", run.seam());
        assertEquals("Plan for RAG", agentic.result());
        assertEquals("LangChain4j agentic demo", agentic.seam());
    }
}
