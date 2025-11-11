package com.sorina.taskflow.dto;

import com.sorina.taskflow.enums.ProjectRole;

import java.util.UUID;

public record ProjectMemberDTO(
        UUID userId,
        String username,
        String email,
        String firstName,
        String lastName,
        ProjectRole role
) {}
