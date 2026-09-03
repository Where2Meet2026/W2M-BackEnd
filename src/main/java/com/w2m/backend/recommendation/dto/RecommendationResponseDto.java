package com.w2m.backend.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationResponseDto {
    private Long meetingId;
    private int totalParticipants;
    private List<AvailableSlotDto> recommendedSlots;
}
