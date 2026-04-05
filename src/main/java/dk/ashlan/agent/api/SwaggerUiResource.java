package dk.ashlan.agent.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import java.net.URI;

@Path("/swagger-ui")
public class SwaggerUiResource {
    @GET
    public Response redirectToSwaggerUiIndex() {
        return Response.seeOther(URI.create("/swagger-ui/")).build();
    }
}
