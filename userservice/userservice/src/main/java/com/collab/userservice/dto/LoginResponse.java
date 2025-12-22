package com.collab.userservice.dto;

public class LoginResponse {
    public String token; // We added this
    public Long id;
    public String username;
    public String email;

    public LoginResponse(String token, Long id, String username, String email) {
        this.token = token;
        this.id = id;
        this.username = username;
        this.email = email;
    }
}