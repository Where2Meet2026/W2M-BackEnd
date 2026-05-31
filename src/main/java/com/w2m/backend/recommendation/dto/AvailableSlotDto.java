package com.w2m.backend.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableSlotDto {
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private int availableCount;
    private List<String> participantNames;
}
