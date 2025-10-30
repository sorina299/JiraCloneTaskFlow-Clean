package com.sorina.taskflow.dto;

public record RegisterResponseDTO(
        AuthenticationResponseDTO tokens,
        UserResponseDTO user
) {}
