package com.sorina.taskflow.dto;

import com.sorina.taskflow.enums.IssuePriority;
import com.sorina.taskflow.enums.IssueStatus;
import com.sorina.taskflow.enums.IssueType;

import java.time.LocalDateTime;
import java.util.UUID;

public record IssueDTO(
        UUID id,
        String title,
        String description,
        IssueType type,
        IssueStatus status,
        IssuePriority priority,
        UUID projectId,
        UUID reporterId,
        UUID assigneeId,
        UUID parentIssueId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}