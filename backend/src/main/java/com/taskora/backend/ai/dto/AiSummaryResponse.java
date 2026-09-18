package com.taskora.backend.ai.dto;

import java.time.Instant;

public record AiSummaryResponse(String summary, Instant generatedAt) {
}
