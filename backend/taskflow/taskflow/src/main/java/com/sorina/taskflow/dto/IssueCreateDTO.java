package com.sorina.taskflow.dto;

import com.sorina.taskflow.enums.IssuePriority;
import com.sorina.taskflow.enums.IssueStatus;
import com.sorina.taskflow.enums.IssueType;

import java.util.UUID;

public record IssueCreateDTO(
        String title,
        String description,
        IssueType type,
        IssueStatus status,
        IssuePriority priority,
        UUID assigneeId,
        UUID parentIssueId
) {}
