package com.tomabot.model.dto;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class SessionStatus {
    private Long sessionId;
    private Integer totalMinutes;
    private Integer elapsedMinutes;
    private Integer remainingMinutes;
    private Instant startTime;
}