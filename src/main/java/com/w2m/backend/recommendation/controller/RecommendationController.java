package com.w2m.backend.recommendation.controller;

import com.w2m.backend.recommendation.dto.RecommendationResponseDto;
import com.w2m.backend.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/meetings/{meetingId}/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping
    public ResponseEntity<RecommendationResponseDto> getRecommendations(@PathVariable Long meetingId) {
        return ResponseEntity.ok(recommendationService.getRecommendations(meetingId));
    }
}
