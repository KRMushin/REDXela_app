package com.redxela.backend.health.dto;

public record HealthResponse(
        String status,
        String application,
        String message
) {}
