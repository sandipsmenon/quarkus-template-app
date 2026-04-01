package com.template.quarkus.client;

import com.template.quarkus.dto.ExternalPostDto;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

/**
 * Quarkus REST Client for https://jsonplaceholder.typicode.com
 *
 * Base URL is configured in application.properties:
 *   quarkus.rest-client."com.template.quarkus.client.JsonPlaceholderClient".url=...
 */
@RegisterRestClient
@Path("/posts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface JsonPlaceholderClient {

    @GET
    List<ExternalPostDto> getAllPosts();

    @GET
    @Path("/{id}")
    ExternalPostDto getPostById(@PathParam("id") Integer id);

    @GET
    List<ExternalPostDto> getPostsByUser(@QueryParam("userId") Integer userId);
}
