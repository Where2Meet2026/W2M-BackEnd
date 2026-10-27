package com.w2m.backend.review.repository;

import com.w2m.backend.review.entity.Review;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    // 특정 장소에 누적된 리뷰 목록 (GET /api/meetings/{meetingId}/reviews), 최신순. 다른 모임에서 쓴 리뷰도 포함한다.
    List<Review> findByPlaceIdOrderByCreatedAtDesc(String placeId);

    // 특정 장소의 누적 리뷰 (후보 "최적" 점수 계산 등에서 사용)
    List<Review> findByPlaceId(String placeId);

    // 한 사용자가 한 장소에 이미 리뷰했는지 (1인 1리뷰)
    boolean existsByPlaceIdAndUserId(String placeId, Long userId);

    // 내가 작성한 리뷰 목록 (내 방 목록의 reviewed 계산에 사용)
    List<Review> findByUserId(Long userId);

    // --- LLM 설명 생성용: 코멘트가 실제로 있는 리뷰만 대상으로 한다 ---

    @Query("select count(r) from Review r where r.placeId = :placeId and r.comment is not null and trim(r.comment) <> ''")
    long countUsableByPlaceId(@Param("placeId") String placeId);

    @Query("select avg(r.rating) from Review r where r.placeId = :placeId")
    Double averageRatingByPlaceId(@Param("placeId") String placeId);

    // 최신순으로 pageable 크기만큼의 코멘트
    @Query("select r.comment from Review r where r.placeId = :placeId and r.comment is not null and trim(r.comment) <> '' order by r.createdAt desc")
    List<String> findUsableComments(@Param("placeId") String placeId, Pageable pageable);
}
