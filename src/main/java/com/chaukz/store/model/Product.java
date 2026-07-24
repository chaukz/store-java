package com.chaukz.store.model;

public class Product {
    private String name;
    private String description;
    private String image;
    private Category category;

    public Product(String name, String description, String image, Category category) {
        this.name = name;
        this.description = description;
        this.image = image;
        this.category = category;
    }
    public String getName() {
        return name;
    }
    public String getDescription() {
        return description;
    }
    public String getImage() {
        return image;
    }
    public Category getCategory() {
        return category;
    }

    public void setName(String name) {
        this.name = name;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public void setImage(String image) {
        this.image = image;
    }
    public void setCategory(Category category) {
        this.category = category;
    }

    public String toString() {
        return super.toString();
    }
}
