package com.template.quarkus.exception;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestResponse;

import java.util.List;

@Provider
public class GlobalExceptionHandler implements ExceptionMapper<Exception> {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionHandler.class);

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(Exception exception) {
        String path = uriInfo != null ? uriInfo.getPath() : "unknown";

        if (exception instanceof ProductNotFoundException ex) {
            LOG.debugf("Product not found: %s", ex.getMessage());
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ErrorResponse.of(404, "NOT_FOUND", ex.getMessage(), path))
                    .build();
        }

        if (exception instanceof DuplicateSkuException ex) {
            LOG.debugf("Duplicate SKU: %s", ex.getMessage());
            return Response.status(Response.Status.CONFLICT)
                    .entity(ErrorResponse.of(409, "CONFLICT", ex.getMessage(), path))
                    .build();
        }

        if (exception instanceof ConstraintViolationException ex) {
            LOG.debugf("Validation error: %s", ex.getMessage());
            ErrorResponse errorResponse = ErrorResponse.of(400, "VALIDATION_ERROR",
                    "Request validation failed", path);
            errorResponse.fieldErrors = ex.getConstraintViolations().stream()
                    .map(cv -> {
                        String field = cv.getPropertyPath().toString();
                        // Strip method name prefix (e.g. "create.request.price" → "price")
                        if (field.contains(".")) {
                            field = field.substring(field.lastIndexOf('.') + 1);
                        }
                        return new ErrorResponse.FieldError(field, cv.getInvalidValue(), cv.getMessage());
                    })
                    .toList();
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorResponse)
                    .build();
        }

        if (exception instanceof jakarta.ws.rs.NotFoundException) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ErrorResponse.of(404, "NOT_FOUND", "Resource not found", path))
                    .build();
        }

        if (exception instanceof jakarta.ws.rs.BadRequestException) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponse.of(400, "BAD_REQUEST", exception.getMessage(), path))
                    .build();
        }

        LOG.errorf(exception, "Unhandled exception on path: %s", path);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(ErrorResponse.of(500, "INTERNAL_SERVER_ERROR",
                        "An unexpected error occurred. Please try again later.", path))
                .build();
    }
}
