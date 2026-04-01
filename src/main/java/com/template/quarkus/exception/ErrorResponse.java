package com.template.quarkus.exception;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Standard error response body")
public class ErrorResponse {

    @Schema(description = "HTTP status code", example = "404")
    public int status;

    @Schema(description = "Short error category", example = "NOT_FOUND")
    public String error;

    @Schema(description = "Human-readable error message", example = "Product not found with ID: 42")
    public String message;

    @Schema(description = "Request path that triggered the error", example = "/api/products/42")
    public String path;

    @Schema(description = "Timestamp when the error occurred")
    public LocalDateTime timestamp;

    @Schema(description = "Validation error details (only present on 400 responses)")
    public List<FieldError> fieldErrors;

    public ErrorResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public static ErrorResponse of(int status, String error, String message, String path) {
        ErrorResponse response = new ErrorResponse();
        response.status = status;
        response.error = error;
        response.message = message;
        response.path = path;
        return response;
    }

    @Schema(description = "Individual field validation error")
    public static class FieldError {
        @Schema(description = "Field name that failed validation", example = "price")
        public String field;

        @Schema(description = "Rejected value", example = "-5.0")
        public Object rejectedValue;

        @Schema(description = "Validation message", example = "Price must be greater than 0")
        public String message;

        public FieldError(String field, Object rejectedValue, String message) {
            this.field = field;
            this.rejectedValue = rejectedValue;
            this.message = message;
        }
    }
}
