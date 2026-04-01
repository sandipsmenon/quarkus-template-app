package com.template.quarkus.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProductResourceTest {

    private static final String PRODUCTS_PATH = "/api/products";

    private static Long createdProductId;

    // -------------------------------------------------------------------------
    // POST — create
    // -------------------------------------------------------------------------
    @Test
    @Order(1)
    void createProduct_shouldReturn201() {
        String body = """
            {
              "name": "Test Headphones",
              "description": "Unit test product",
              "sku": "TEST-SKU-001",
              "price": 99.99,
              "stockQuantity": 50,
              "category": "Electronics",
              "active": true
            }
            """;

        createdProductId = given()
                .contentType(ContentType.JSON)
                .body(body)
            .when()
                .post(PRODUCTS_PATH)
            .then()
                .statusCode(201)
                .body("name", equalTo("Test Headphones"))
                .body("sku", equalTo("TEST-SKU-001"))
                .body("price", equalTo(99.99f))
                .body("category", equalTo("Electronics"))
                .body("active", equalTo(true))
                .body("id", notNullValue())
                .extract().jsonPath().getLong("id");
    }

    @Test
    @Order(2)
    void createProduct_duplicateSku_shouldReturn409() {
        String body = """
            {
              "name": "Duplicate SKU Product",
              "sku": "TEST-SKU-001",
              "price": 10.00,
              "stockQuantity": 5,
              "category": "Test"
            }
            """;

        given()
                .contentType(ContentType.JSON)
                .body(body)
            .when()
                .post(PRODUCTS_PATH)
            .then()
                .statusCode(409)
                .body("error", equalTo("CONFLICT"));
    }

    @Test
    @Order(3)
    void createProduct_missingName_shouldReturn400() {
        String body = """
            {
              "sku": "NO-NAME-001",
              "price": 10.00,
              "stockQuantity": 5,
              "category": "Test"
            }
            """;

        given()
                .contentType(ContentType.JSON)
                .body(body)
            .when()
                .post(PRODUCTS_PATH)
            .then()
                .statusCode(400)
                .body("error", equalTo("VALIDATION_ERROR"))
                .body("fieldErrors", not(empty()));
    }

    // -------------------------------------------------------------------------
    // GET — list
    // -------------------------------------------------------------------------
    @Test
    @Order(4)
    void getAllProducts_shouldReturn200() {
        given()
            .when()
                .get(PRODUCTS_PATH)
            .then()
                .statusCode(200)
                .body("content", not(empty()))
                .body("page", equalTo(0))
                .body("size", equalTo(10))
                .body("totalElements", greaterThan(0));
    }

    @Test
    @Order(5)
    void getAllProducts_withPagination_shouldReturn200() {
        given()
                .queryParam("page", 0)
                .queryParam("size", 5)
            .when()
                .get(PRODUCTS_PATH)
            .then()
                .statusCode(200)
                .body("size", equalTo(5))
                .body("page", equalTo(0));
    }

    // -------------------------------------------------------------------------
    // GET — by ID
    // -------------------------------------------------------------------------
    @Test
    @Order(6)
    void getProductById_shouldReturn200() {
        given()
            .when()
                .get(PRODUCTS_PATH + "/" + createdProductId)
            .then()
                .statusCode(200)
                .body("id", equalTo(createdProductId.intValue()))
                .body("name", equalTo("Test Headphones"));
    }

    @Test
    @Order(7)
    void getProductById_notFound_shouldReturn404() {
        given()
            .when()
                .get(PRODUCTS_PATH + "/99999")
            .then()
                .statusCode(404)
                .body("error", equalTo("NOT_FOUND"));
    }

    // -------------------------------------------------------------------------
    // GET — by category
    // -------------------------------------------------------------------------
    @Test
    @Order(8)
    void getByCategory_shouldReturn200() {
        given()
            .when()
                .get(PRODUCTS_PATH + "/category/Electronics")
            .then()
                .statusCode(200)
                .body("$", not(empty()))
                .body("[0].category", equalTo("Electronics"));
    }

    // -------------------------------------------------------------------------
    // GET — search
    // -------------------------------------------------------------------------
    @Test
    @Order(9)
    void searchProducts_shouldReturn200() {
        given()
                .queryParam("q", "headphone")
            .when()
                .get(PRODUCTS_PATH + "/search")
            .then()
                .statusCode(200)
                .body("$", not(empty()));
    }

    // -------------------------------------------------------------------------
    // PUT — update
    // -------------------------------------------------------------------------
    @Test
    @Order(10)
    void updateProduct_shouldReturn200() {
        String body = """
            {
              "name": "Updated Headphones",
              "description": "Updated description",
              "sku": "TEST-SKU-001",
              "price": 129.99,
              "stockQuantity": 75,
              "category": "Electronics",
              "active": true
            }
            """;

        given()
                .contentType(ContentType.JSON)
                .body(body)
            .when()
                .put(PRODUCTS_PATH + "/" + createdProductId)
            .then()
                .statusCode(200)
                .body("name", equalTo("Updated Headphones"))
                .body("price", equalTo(129.99f))
                .body("stockQuantity", equalTo(75));
    }

    @Test
    @Order(11)
    void updateProduct_notFound_shouldReturn404() {
        String body = """
            {
              "name": "Ghost Product",
              "sku": "GHOST-001",
              "price": 1.00,
              "stockQuantity": 1,
              "category": "Test"
            }
            """;

        given()
                .contentType(ContentType.JSON)
                .body(body)
            .when()
                .put(PRODUCTS_PATH + "/99999")
            .then()
                .statusCode(404);
    }

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------
    @Test
    @Order(12)
    void deleteProduct_shouldReturn204() {
        given()
            .when()
                .delete(PRODUCTS_PATH + "/" + createdProductId)
            .then()
                .statusCode(204);
    }

    @Test
    @Order(13)
    void deleteProduct_alreadyDeleted_shouldReturn404() {
        given()
            .when()
                .delete(PRODUCTS_PATH + "/" + createdProductId)
            .then()
                .statusCode(404);
    }

    @Test
    @Order(14)
    void getDeletedProduct_shouldReturn404() {
        given()
            .when()
                .get(PRODUCTS_PATH + "/" + createdProductId)
            .then()
                .statusCode(404);
    }
}
