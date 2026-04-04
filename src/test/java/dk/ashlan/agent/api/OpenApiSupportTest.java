package dk.ashlan.agent.api;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiSupportTest {
    @Test
    void applicationPropertiesExposeOpenApiContractSettings() throws Exception {
        assertTrue(AgentResource.class.isAnnotationPresent(jakarta.ws.rs.Path.class));
        assertTrue(ToolResource.class.isAnnotationPresent(jakarta.ws.rs.Path.class));
        assertTrue(RuntimeInspectionResource.class.isAnnotationPresent(jakarta.ws.rs.Path.class));
        assertTrue(RagResource.class.isAnnotationPresent(jakarta.ws.rs.Path.class));
        assertTrue(CompanionResource.class.isAnnotationPresent(jakarta.ws.rs.Path.class));
        assertTrue(AdminEvaluationResource.class.isAnnotationPresent(jakarta.ws.rs.Path.class));
        assertTrue(GaiaEvaluationResource.class.isAnnotationPresent(jakarta.ws.rs.Path.class));
        assertTrue(CodeAgentResource.class.isAnnotationPresent(jakarta.ws.rs.Path.class));
        assertTrue(MultiAgentResource.class.isAnnotationPresent(jakarta.ws.rs.Path.class));
        assertTrue(WorkflowResource.class.isAnnotationPresent(jakarta.ws.rs.Path.class));
        String properties = Files.readString(Path.of("src/main/resources/application.properties"));
        assertTrue(properties.contains("quarkus.smallrye-openapi.path=/openapi"));
        assertTrue(properties.contains("quarkus.smallrye-openapi.info-title=Quarkus Agent Edition API"));
        assertTrue(properties.contains("Swagger documents the HTTP-exposed outer seams"));
    }

    @Test
    void applicationPropertiesEnableSwaggerUiOnTheConfiguredPath() throws Exception {
        String properties = Files.readString(Path.of("src/main/resources/application.properties"));
        assertTrue(properties.contains("quarkus.swagger-ui.path=/swagger-ui"));
        assertTrue(properties.contains("quarkus.swagger-ui.always-include=true"));
    }

    @Test
    void keyOperationsExposeChapterMappingInSwaggerText() throws Exception {
        assertOperationContains(AgentResource.class, "runAgent", "Book chapter mapping: cross-cutting runtime seam");
        assertOperationContains(AgentResource.class, "step", "Book chapter mapping: chapter 4 ReAct step seam");
        assertOperationContains(AgentResource.class, "runStructured", "Book chapter mapping: chapter 4 structured-output seam");
        assertOperationContains(ToolResource.class, "listTools", "Book chapter mapping: cross-cutting runtime seam");
        assertOperationContains(AdminEvaluationResource.class, "run", "Book chapter: 10");
        assertOperationContains(AdminEvaluationResource.class, "trace", "Book chapter: 10");
        assertOperationContains(GaiaEvaluationResource.class, "run", "Book chapter: 10");
        assertOperationContains(GaiaEvaluationResource.class, "task", "Book chapter: 10");
        assertOperationContains(GaiaEvaluationResource.class, "runLookup", "Book chapter: 10");
        assertOperationContains(CodeAgentResource.class, "run", "Book chapter: 8");
        assertOperationContains(MultiAgentResource.class, "run", "Book chapter: 9");
        assertOperationContains(WorkflowResource.class, "demo", "Book chapter: 7");
        assertOperationContains(CompanionResource.class, "run", "Book chapter mapping: chapter 7 companion seam");
        assertOperationContains(CompanionResource.class, "agenticDemo", "Book chapter mapping: chapter 7 companion seam");
        assertOperationContains(RuntimeInspectionResource.class, "health", "Book chapter mapping: cross-cutting runtime seam");
        assertOperationContains(RuntimeInspectionResource.class, "readiness", "Book chapter mapping: cross-cutting runtime seam");
        assertOperationContains(RuntimeInspectionResource.class, "liveness", "Book chapter mapping: cross-cutting runtime seam");
        assertOperationContains(RuntimeInspectionResource.class, "session", "Book chapter: 6");
        assertOperationContains(RuntimeInspectionResource.class, "memory", "Book chapter: 6");
        assertOperationContains(RuntimeInspectionResource.class, "trace", "Book chapter mapping: chapter 4 runtime trace inspection seam");
        assertOperationContains(RagResource.class, "ingest", "Book chapter: 5");
        assertOperationContains(RagResource.class, "ingestPath", "Book chapter: 5");
        assertOperationContains(RagResource.class, "query", "Book chapter: 5");
    }

    private static void assertOperationContains(Class<?> type, String methodName, String expected) throws Exception {
        Method method = type.getDeclaredMethod(methodName, methodParameterTypes(type, methodName));
        Operation operation = method.getAnnotation(Operation.class);
        assertTrue(operation != null, type.getSimpleName() + "." + methodName + " is missing @Operation");
        assertTrue(operation.description().contains(expected), type.getSimpleName() + "." + methodName + " should contain '" + expected + "'");
    }

    private static Class<?>[] methodParameterTypes(Class<?> type, String methodName) {
        for (Method method : type.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                return method.getParameterTypes();
            }
        }
        throw new IllegalArgumentException("No method named " + methodName + " on " + type.getName());
    }
}
