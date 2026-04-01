package com.template.quarkus.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Product data returned from the API")
public class ProductResponse {

    @Schema(description = "Unique product identifier", example = "1")
    public Long id;

    @Schema(description = "Product name", example = "Wireless Headphones")
    public String name;

    @Schema(description = "Product description", example = "High-quality wireless headphones with noise cancellation")
    public String description;

    @Schema(description = "Stock Keeping Unit", example = "WH-1000XM5")
    public String sku;

    @Schema(description = "Product price in USD", example = "349.99")
    public BigDecimal price;

    @Schema(description = "Available stock quantity", example = "150")
    public Integer stockQuantity;

    @Schema(description = "Product category", example = "Electronics")
    public String category;

    @Schema(description = "Whether the product is active/visible", example = "true")
    public boolean active;

    @Schema(description = "Timestamp when the product was created")
    public LocalDateTime createdAt;

    @Schema(description = "Timestamp when the product was last updated")
    public LocalDateTime updatedAt;
}
