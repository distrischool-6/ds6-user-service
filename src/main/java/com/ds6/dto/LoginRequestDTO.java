package com.ds6.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(
    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email should be valid")    
    String email,

    @NotBlank(message = "Password cannot be blank")
    String password
) {}