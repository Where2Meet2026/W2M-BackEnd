package com.w2m.backend.review.repository;

import com.w2m.backend.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    // 특정 모임 흐름에서 작성된 리뷰 목록 (GET /api/meetings/{meetingId}/reviews), 최신순
    List<Review> findByMeetingIdOrderByCreatedAtDesc(Long meetingId);

    // 특정 장소의 누적 리뷰 (후보 "최적" 점수 계산 등에서 사용)
    List<Review> findByPlaceId(String placeId);

    // 한 사용자가 한 장소에 이미 리뷰했는지 (1인 1리뷰)
    boolean existsByPlaceIdAndUserId(String placeId, Long userId);
}
