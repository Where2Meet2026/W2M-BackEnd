package com.w2m.backend.review.dto.response;

import com.w2m.backend.review.entity.Review;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 익명 리뷰라 작성자 정보는 의도적으로 응답에 포함하지 않는다.
@Getter
@Builder
public class ReviewResponse {

    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;

    public static ReviewResponse from(Review review) {
        return ReviewResponse.builder()
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
