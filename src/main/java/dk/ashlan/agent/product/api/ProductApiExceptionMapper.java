package dk.ashlan.agent.product.api;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.time.Instant;

@Provider
public class ProductApiExceptionMapper implements ExceptionMapper<ProductApiException> {
    @Override
    public Response toResponse(ProductApiException exception) {
        ProductApiErrorResponse response = new ProductApiErrorResponse(
                Instant.now(),
                exception.status(),
                exception.errorCode(),
                exception.getMessage(),
                exception.conversationId(),
                exception.requestId()
        );
        return Response.status(exception.status())
                .type(MediaType.APPLICATION_JSON)
                .entity(response)
                .build();
    }
}
