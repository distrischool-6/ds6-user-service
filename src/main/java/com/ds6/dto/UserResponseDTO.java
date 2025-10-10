package com.ds6.dto;

import java.util.UUID;

import com.ds6.model.Role;

public record UserResponseDTO(
    UUID id,
    String email, 
    Role role
) {}
