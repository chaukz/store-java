package com.chaukz.store.model;
import java.util.Date;
import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //Fields
    private String name;
    private String lastName;
    private String phone;
    private Date dob;
    private String email;
    private String password;

    public User() {}

    public User(String name, String lastName, String phone, Date dob, String email, String password){
        this.name = name;
        this.lastName = lastName;
        this.phone = phone;
        this.dob = dob;
        this.email = email;
        this.password = password;
    }

    public String getName() {
        return name;
    }
    public String getLastName() {
        return lastName;
    }
    public String getPhone() {
        return phone;
    }
    public Date getDob() {
        return dob;
    }
    public String getEmail() {
        return email;
    }
    public String getPassword() {
        return password;
    }

    //Setters
    public void setName(String name) {
        this.name = name;
    }
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    public void setPhone(String phone) {
        this.phone = phone;
    }
    public void setDob(Date dob) {
        this.dob = dob;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public void setPassword(String password) {
        this.password = password;
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String toString() {
        return super.toString();
    }
}
