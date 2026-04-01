package com.template.quarkus.dto;

import jakarta.validation.constraints.*;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Request payload for creating or updating a product")
public class ProductRequest {

    @Schema(description = "Product name", example = "Wireless Headphones", required = true)
    @NotBlank(message = "Name is required")
    @Size(min = 1, max = 255, message = "Name must be between 1 and 255 characters")
    public String name;

    @Schema(description = "Product description", example = "High-quality wireless headphones with noise cancellation")
    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    public String description;

    @Schema(description = "Stock Keeping Unit — must be unique", example = "WH-1000XM5", required = true)
    @NotBlank(message = "SKU is required")
    @Size(min = 1, max = 100)
    public String sku;

    @Schema(description = "Product price in USD", example = "349.99", required = true)
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @Digits(integer = 10, fraction = 2)
    public BigDecimal price;

    @Schema(description = "Available stock quantity", example = "150", required = true)
    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity cannot be negative")
    public Integer stockQuantity;

    @Schema(description = "Product category", example = "Electronics", required = true)
    @NotBlank(message = "Category is required")
    @Size(max = 100)
    public String category;

    @Schema(description = "Whether the product is active/visible", example = "true", defaultValue = "true")
    public boolean active = true;
}
