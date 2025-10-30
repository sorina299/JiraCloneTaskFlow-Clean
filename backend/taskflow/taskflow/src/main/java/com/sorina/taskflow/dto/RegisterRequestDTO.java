package com.sorina.taskflow.dto;

public record RegisterRequestDTO(
        String username,
        String email,
        String password,
        String firstName,
        String lastName
) {}

