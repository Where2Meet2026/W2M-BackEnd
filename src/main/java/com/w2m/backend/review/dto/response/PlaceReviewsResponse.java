package com.w2m.backend.review.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

// 후보 장소 하나에 누적된 리뷰 (투표 화면의 "리뷰 확인"용). 작성자 정보는 응답에 포함하지 않는다.
@Getter
@Builder
@AllArgsConstructor
public class PlaceReviewsResponse {
    private String placeName;
    private String placeAddress;
    private List<ReviewResponse> reviews;
}
