package dk.ashlan.agent.product.model;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record ProductReflectionResponse(
        @Schema(description = "Whether the answer passed the lightweight product-quality check.")
        boolean accepted,
        @Schema(description = "Short review feedback when the answer needs improvement.")
        String feedback
) {
}
