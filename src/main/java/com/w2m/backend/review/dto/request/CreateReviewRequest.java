package com.w2m.backend.review.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateReviewRequest {

    @NotNull(message = "별점을 선택해주세요.")
    @Min(value = 1, message = "별점은 1~5점으로 선택해주세요.")
    @Max(value = 5, message = "별점은 1~5점으로 선택해주세요.")
    private Integer rating;

    @Size(max = 500, message = "한줄평은 500자까지 쓸 수 있어요.")
    private String comment;
}
