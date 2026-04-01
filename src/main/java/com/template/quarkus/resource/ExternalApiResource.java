package com.template.quarkus.resource;

import com.template.quarkus.dto.ExternalPostSummary;
import com.template.quarkus.exception.ErrorResponse;
import com.template.quarkus.service.ExternalApiService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/api/external/posts")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "External API", description = "Endpoints that proxy and transform data from jsonplaceholder.typicode.com")
public class ExternalApiResource {

    @Inject
    ExternalApiService externalApiService;

    // -------------------------------------------------------------------------
    // GET /api/external/posts
    // -------------------------------------------------------------------------
    @GET
    @Operation(
        summary = "Get all external posts (transformed)",
        description = "Fetches all posts from jsonplaceholder.typicode.com and returns them as transformed summaries."
    )
    @APIResponses({
        @APIResponse(responseCode = "200", description = "List of post summaries"),
        @APIResponse(responseCode = "502", description = "External API unavailable",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response getAllPosts() {
        List<ExternalPostSummary> summaries = externalApiService.getAllPostSummaries();
        return Response.ok(summaries).build();
    }

    // -------------------------------------------------------------------------
    // GET /api/external/posts/{id}
    // -------------------------------------------------------------------------
    @GET
    @Path("/{id}")
    @Operation(
        summary = "Get a single external post by ID (transformed)",
        description = "Fetches a single post from jsonplaceholder.typicode.com and returns it as a transformed summary."
    )
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Post summary"),
        @APIResponse(responseCode = "404", description = "Post not found on external API",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @APIResponse(responseCode = "502", description = "External API unavailable",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response getPostById(
            @Parameter(description = "Post ID (1–100)", required = true, example = "1")
            @PathParam("id") Integer id) {

        ExternalPostSummary summary = externalApiService.getPostSummaryById(id);
        return Response.ok(summary).build();
    }

    // -------------------------------------------------------------------------
    // GET /api/external/posts?userId={userId}
    // -------------------------------------------------------------------------
    @GET
    @Path("/user/{userId}")
    @Operation(
        summary = "Get external posts by user ID (transformed)",
        description = "Fetches posts authored by a given user from jsonplaceholder.typicode.com."
    )
    @APIResponses({
        @APIResponse(responseCode = "200", description = "List of post summaries for the user"),
        @APIResponse(responseCode = "502", description = "External API unavailable",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response getPostsByUser(
            @Parameter(description = "Author user ID (1–10)", required = true, example = "1")
            @PathParam("userId") Integer userId) {

        List<ExternalPostSummary> summaries = externalApiService.getPostSummariesByUser(userId);
        return Response.ok(summaries).build();
    }
}
