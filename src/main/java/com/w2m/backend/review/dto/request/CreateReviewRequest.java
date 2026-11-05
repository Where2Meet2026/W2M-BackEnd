package com.w2m.backend.review.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateReviewRequest {

    @NotNull(message = "별점은 필수입니다.")
    @DecimalMin(value = "0.5", message = "별점은 0.5 이상이어야 합니다.")
    @DecimalMax(value = "5.0", message = "별점은 5 이하여야 합니다.")
    private Double rating;

    @Size(max = 500, message = "한줄평은 500자 이하여야 합니다.")
    private String comment;

    // 0.5 단위만 허용한다 (0.5의 배수는 double로 정확히 표현되므로 2배 한 값이 정수인지 보면 된다).
    @JsonIgnore
    @AssertTrue(message = "별점은 0.5 단위로만 입력할 수 있습니다.")
    public boolean isRatingHalfStep() {
        return rating == null || (rating * 2) == Math.rint(rating * 2);
    }
}
