package com.chaukz.store.model;

public class ProductVariant {
    private String name;
    private String description;
    private String image;

    public ProductVariant(String name, String description, String image) {
        this.name = name;
        this.description = description;
        this.image = image;
    }

    public String toString() {
        return super.toString();
    }
}
