package org.example.mvc.bean;

public class User {
    public String username;
    public String password;

    public String name;
    public String description;

    public User() {
    }

    public User(String username, String password, String name, String description) {
        this.username = username;
        this.password = password;
        this.name = name;
        this.description = description;
    }
}
