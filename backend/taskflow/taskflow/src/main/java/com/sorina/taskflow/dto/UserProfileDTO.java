package com.sorina.taskflow.dto;

public record UserProfileDTO(
        String firstName,
        String lastName,
        String pronouns,
        String jobTitle,
        String description,
        String profilePictureUrl
) {}
