package com.chaukz.store.model;

public class Category {
    private String name;
    private String description;
    private String image;

    public Category(String name, String description, String image) {
        this.name = name;
        this.description = description;
        this.image = image;
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

    public void setDescription(String description) {
        this.description = description;
    }
    public void setImage(String image) {
        this.image = image;
    }
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
