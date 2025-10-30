package com.sorina.taskflow.dto;

import com.sorina.taskflow.enums.RoleType;

import java.util.Set;

public record UserResponseDTO(
        String id,
        String username,
        String email,
        String firstName,
        String lastName,
        Set<RoleType> roles,
        String profilePictureUrl
) {}
