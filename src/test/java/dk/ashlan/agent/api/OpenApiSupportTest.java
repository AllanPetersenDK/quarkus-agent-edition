package dk.ashlan.agent.api;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

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
}
