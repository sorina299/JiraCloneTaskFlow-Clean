package com.sorina.taskflow.dto;

import java.util.UUID;

public record ProjectDTO(
        UUID id,
        String key,
        String name,
        String description
) {}
