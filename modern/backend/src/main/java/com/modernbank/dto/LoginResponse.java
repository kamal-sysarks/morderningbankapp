package com.modernbank.dto;

public class LoginResponse {
    private String token;
    private String username;
    private String employeeName;

    public LoginResponse(String token, String username, String employeeName) {
        this.token = token;
        this.username = username;
        this.employeeName = employeeName;
    }

    public String getToken() { return token; }
    public String getUsername() { return username; }
    public String getEmployeeName() { return employeeName; }
}
