package dk.ashlan.agent.product.api;

public class ProductApiException extends RuntimeException {
    private final int status;
    private final String errorCode;
    private final String conversationId;
    private final String requestId;

    public ProductApiException(int status, String errorCode, String message, String conversationId, String requestId, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.errorCode = errorCode;
        this.conversationId = conversationId;
        this.requestId = requestId;
    }

    public int status() {
        return status;
    }

    public String errorCode() {
        return errorCode;
    }

    public String conversationId() {
        return conversationId;
    }

    public String requestId() {
        return requestId;
    }
}
