package com.template.quarkus.resource;

import com.template.quarkus.dto.PagedResponse;
import com.template.quarkus.dto.ProductRequest;
import com.template.quarkus.dto.ProductResponse;
import com.template.quarkus.exception.ErrorResponse;
import com.template.quarkus.service.ProductService;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.net.URI;
import java.util.List;

/**
 * Product catalog REST API — enhanced with:
 *
 * FEATURE: JWT Role-Based Access Control (@RolesAllowed).
 *
 * Jakarta Security (and MicroProfile JWT) let you restrict endpoints by role.
 * The role comes from the "groups" claim inside the JWT, populated at login.
 *
 * Key annotations:
 *   @PermitAll         — no token required (public endpoint)
 *   @RolesAllowed("User")  — any authenticated user (role: User)
 *   @RolesAllowed("Admin") — only admin users (role: Admin)
 *   @DenyAll           — always denied (useful for disabling endpoints)
 *
 * Policy application order (from broadest to most specific):
 *   class-level → method-level (method-level takes precedence)
 *
 * If no security annotation is present, Quarkus defaults to:
 *   - authenticated required (if quarkus.security.jaxrs.deny-unannotated-endpoints=true)
 *   - permit all (default)
 *
 * See application.properties:
 *   mp.jwt.verify.publickey.location=publicKey.pem
 *   mp.jwt.verify.issuer=https://quarkus-template-app
 *
 * How to test:
 *   1. POST /api/auth/login  { "username": "admin", "password": "admin123" }
 *   2. Copy the "token" from the response
 *   3. Add header: Authorization: Bearer <token>
 *   4. Call any protected endpoint
 */
@Path("/api/products")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Products", description = "Product catalog management endpoints")
public class ProductResource {

    @Inject
    ProductService productService;

    @Context
    UriInfo uriInfo;

    // -------------------------------------------------------------------------
    // GET /api/products — public read access
    // -------------------------------------------------------------------------
    @GET
    @PermitAll  // Anyone can browse products — no token required
    @Operation(
        summary = "List all active products",
        description = "Returns a paginated list of all active products. No authentication required."
    )
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Paginated product list",
            content = @Content(schema = @Schema(implementation = PagedResponse.class))),
        @APIResponse(responseCode = "400", description = "Invalid pagination parameters",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response getAll(
            @Parameter(description = "Page number (0-based)", example = "0")
            @QueryParam("page") @DefaultValue("0") @Min(0) int page,

            @Parameter(description = "Number of items per page", example = "10")
            @QueryParam("size") @DefaultValue("10") @Min(1) @Max(100) int size) {

        PagedResponse<ProductResponse> response = productService.getAll(page, size);
        return Response.ok(response).build();
    }

    // -------------------------------------------------------------------------
    // GET /api/products/{id} — public read access
    // -------------------------------------------------------------------------
    @GET
    @Path("/{id}")
    @PermitAll  // Product details are publicly readable
    @Operation(summary = "Get a product by ID")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Product found",
            content = @Content(schema = @Schema(implementation = ProductResponse.class))),
        @APIResponse(responseCode = "404", description = "Product not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response getById(
            @Parameter(description = "Product ID", required = true, example = "1")
            @PathParam("id") Long id) {

        ProductResponse product = productService.getById(id);
        return Response.ok(product).build();
    }

    // -------------------------------------------------------------------------
    // GET /api/products/category/{category} — public
    // -------------------------------------------------------------------------
    @GET
    @Path("/category/{category}")
    @PermitAll
    @Operation(summary = "Get products by category")
    @APIResponse(responseCode = "200", description = "List of products in the given category")
    public Response getByCategory(
            @Parameter(description = "Product category", required = true, example = "Electronics")
            @PathParam("category") String category) {

        List<ProductResponse> products = productService.getByCategory(category);
        return Response.ok(products).build();
    }

    // -------------------------------------------------------------------------
    // GET /api/products/search?q=keyword — public
    // -------------------------------------------------------------------------
    @GET
    @Path("/search")
    @PermitAll
    @Operation(summary = "Search products by name keyword")
    @APIResponse(responseCode = "200", description = "Matching products")
    public Response search(
            @Parameter(description = "Search keyword", required = true, example = "headphone")
            @QueryParam("q") @NotBlank String keyword) {

        List<ProductResponse> products = productService.search(keyword);
        return Response.ok(products).build();
    }

    // -------------------------------------------------------------------------
    // POST /api/products — requires "User" or "Admin" role
    // -------------------------------------------------------------------------
    @POST
    @RolesAllowed({"User", "Admin"})  // Any authenticated user may create products
    @Operation(
        summary = "Create a new product",
        description = "Requires authentication. Any logged-in user (role: User or Admin) may create products.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @APIResponses({
        @APIResponse(responseCode = "201", description = "Product created",
            content = @Content(schema = @Schema(implementation = ProductResponse.class))),
        @APIResponse(responseCode = "400", description = "Validation error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @APIResponse(responseCode = "401", description = "Not authenticated — include Bearer token"),
        @APIResponse(responseCode = "403", description = "Insufficient role"),
        @APIResponse(responseCode = "409", description = "SKU already exists",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response create(@Valid ProductRequest request) {
        ProductResponse created = productService.create(request);
        URI location = uriInfo.getAbsolutePathBuilder()
                .path(String.valueOf(created.id))
                .build();
        return Response.created(location).entity(created).build();
    }

    // -------------------------------------------------------------------------
    // PUT /api/products/{id} — requires "Admin" role
    // -------------------------------------------------------------------------
    @PUT
    @Path("/{id}")
    @RolesAllowed("Admin")  // Only admins may update products
    @Operation(
        summary = "Update an existing product",
        description = "Requires Admin role. Only administrators can modify existing products.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Product updated",
            content = @Content(schema = @Schema(implementation = ProductResponse.class))),
        @APIResponse(responseCode = "400", description = "Validation error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @APIResponse(responseCode = "401", description = "Not authenticated"),
        @APIResponse(responseCode = "403", description = "Admin role required"),
        @APIResponse(responseCode = "404", description = "Product not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @APIResponse(responseCode = "409", description = "SKU already exists",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response update(
            @Parameter(description = "Product ID", required = true, example = "1")
            @PathParam("id") Long id,
            @Valid ProductRequest request) {

        ProductResponse updated = productService.update(id, request);
        return Response.ok(updated).build();
    }

    // -------------------------------------------------------------------------
    // DELETE /api/products/{id} — requires "Admin" role
    // -------------------------------------------------------------------------
    @DELETE
    @Path("/{id}")
    @RolesAllowed("Admin")  // Only admins may delete products
    @Operation(
        summary = "Delete a product",
        description = """
            Soft-deletes the product by marking it inactive.
            The record is retained in the database.
            Requires Admin role.
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @APIResponses({
        @APIResponse(responseCode = "204", description = "Product deleted"),
        @APIResponse(responseCode = "401", description = "Not authenticated"),
        @APIResponse(responseCode = "403", description = "Admin role required"),
        @APIResponse(responseCode = "404", description = "Product not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response delete(
            @Parameter(description = "Product ID", required = true, example = "1")
            @PathParam("id") Long id) {

        productService.delete(id);
        return Response.noContent().build();
    }
}
