package com.w2m.backend.review.controller;

import com.w2m.backend.auth.jwt.CustomUserDetails;
import com.w2m.backend.review.dto.request.CreateReviewRequest;
import com.w2m.backend.review.dto.response.PlaceReviewsResponse;
import com.w2m.backend.review.dto.response.ReviewResponse;
import com.w2m.backend.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/meetings/{meetingId}/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Long createReview(
            @PathVariable Long meetingId,
            @Valid @RequestBody CreateReviewRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUser().getId();
        return reviewService.createReview(meetingId, request, userId);
    }

    @GetMapping
    public List<ReviewResponse> getReviews(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUser().getId();
        return reviewService.getReviews(meetingId, userId);
    }

    // GET /api/meetings/{meetingId}/reviews/candidates/{candidateId} — 투표 중인 후보 장소의 누적 리뷰
    @GetMapping("/candidates/{candidateId}")
    public PlaceReviewsResponse getCandidateReviews(
            @PathVariable Long meetingId,
            @PathVariable Long candidateId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUser().getId();
        return reviewService.getCandidateReviews(meetingId, candidateId, userId);
    }
}
