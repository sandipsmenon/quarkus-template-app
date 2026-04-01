package com.template.quarkus.exception;

public class DuplicateSkuException extends RuntimeException {

    private final String sku;

    public DuplicateSkuException(String sku) {
        super("A product with SKU '" + sku + "' already exists");
        this.sku = sku;
    }

    public String getSku() {
        return sku;
    }
}
