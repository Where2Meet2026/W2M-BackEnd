package com.w2m.backend.review.entity;

import com.w2m.backend.auth.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 확정된 장소에 대한 익명 리뷰(별점 + 한줄평).
 *
 * 후보(place_candidates)나 참여자(participants)로 나가는 FK가 없다.
 * - 장소는 카카오 로컬 API의 place 고유 id(place_id) 문자열로만 식별하며,
 *   후보 "최적" 점수 계산은 place_id 로 누적 리뷰를 조회한다.
 * - place_id / place_name / place_address 는 클라이언트가 아니라
 *   meetings.confirmed_place_* 에서 서버가 복사해 넣는다.
 * 덕분에 모임 · 참여자 · 후보 삭제가 리뷰에 아무 영향을 주지 않는다.
 */
@Entity
@Table(
        name = "reviews",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_review_place_user",
                columnNames = {"place_id", "user_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 작성자. 익명이라 응답에는 노출하지 않으며, 중복 방지 + 내부 추적용.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 리뷰 대상 장소 (카카오 place 고유 id). FK 아님.
    @Column(name = "place_id", nullable = false)
    private String placeId;

    @Column(name = "place_name", nullable = false)
    private String placeName;

    @Column(name = "place_address")
    private String placeAddress;

    // 어느 모임 흐름에서 작성된 리뷰인지 (FK 아님, 조회 필터 + breadcrumb).
    @Column(name = "meeting_id", nullable = false)
    private Long meetingId;

    // 별점 1~5 (범위 검증은 요청 DTO에서 처리).
    @Column(nullable = false)
    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Review(User user, String placeId, String placeName, String placeAddress,
                  Long meetingId, Integer rating, String comment) {
        this.user = user;
        this.placeId = placeId;
        this.placeName = placeName;
        this.placeAddress = placeAddress;
        this.meetingId = meetingId;
        this.rating = rating;
        this.comment = comment;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
