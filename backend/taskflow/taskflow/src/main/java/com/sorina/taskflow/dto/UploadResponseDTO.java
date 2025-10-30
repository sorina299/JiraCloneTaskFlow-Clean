package com.sorina.taskflow.dto;

public record UploadResponseDTO (
    int status,
    String message,
    String url
){}
