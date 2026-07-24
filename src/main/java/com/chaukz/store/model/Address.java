package com.chaukz.store.model;

public class Address {
    private String street;
    private String city;
    private String province;
    public int zipCode;

    public Address(String street, String city, String province, int zipCode) {
        this.street = street;
        this.city = city;
        this.province = province;
        this.zipCode = zipCode;
    }
    public String toString() {
        return super.toString();
    }
}
