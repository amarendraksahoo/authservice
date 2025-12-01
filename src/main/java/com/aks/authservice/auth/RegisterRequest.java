package com.aks.authservice.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Set;

import com.aks.authservice.user.RoleType;


@Data
public class RegisterRequest {

    @NotBlank
    private String employeeId;

    @NotBlank
    private String name;

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String password;

    // For now, allow passing roles; later you can restrict from admin side
    private Set<RoleType> roles;
}

