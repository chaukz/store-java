package com.chaukz.store.model;

public class CartItem {
    private Product product;
    private int quantity;
    private ProductVariant variant;
    private double price;
    private double total;
    private boolean isSelected;
    private boolean isAvailable;

    public CartItem(Product product, int quantity, ProductVariant variant, double price, double total, boolean isSelected, boolean isAvailable) {
        this.product = product;
        this.quantity = quantity;
        this.variant = variant;
        this.price = price;
        this.total = total;
        this.isSelected = isSelected;
        this.isAvailable = isAvailable;
    }
    public Product getProduct() {
        return product;
    }
    public int getQuantity() {
        return quantity;
    }
    public ProductVariant getVariant() {
        return variant;
    }
    public double getPrice() {
        return price;
    }
    public double getTotal() {
        return total;
    }
    public boolean isSelected() {
        return isSelected;
    }
    public boolean isAvailable() {
        return isAvailable;
    }
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
    public void setTotal(double total) {
        this.total = total;
    }
    public void setSelected(boolean selected) {
        isSelected = selected;
    }
    public void setAvailable(boolean available) {
        isAvailable = available;
    }
    public String toString() {
        return super.toString();
    }
}
