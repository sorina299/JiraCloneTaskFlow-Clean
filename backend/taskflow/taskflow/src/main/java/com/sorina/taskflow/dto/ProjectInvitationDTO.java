package com.sorina.taskflow.dto;

import com.sorina.taskflow.enums.InvitationStatus;
import com.sorina.taskflow.enums.ProjectRole;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProjectInvitationDTO(
        UUID id,
        UUID projectId,
        String projectKey,
        String projectName,
        ProjectRole role,
        InvitationStatus status,
        LocalDateTime createdAt
) {}
