package com.dom.irk_Backend.model;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;
}
