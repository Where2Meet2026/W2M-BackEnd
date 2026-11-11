package com.w2m.backend.candidate.entity;

import com.w2m.backend.meeting.entity.Meeting;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "place_candidates")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) // LAZY : meeting을 실제로 사용할 때까지 조회x
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    @Column(name = "kakao_place_id", nullable = false)
    private String kakaoPlaceId;

    @Column(nullable = false)
    private String placeName;

    @Column(nullable = false)
    private String address;

    // 카카오 카테고리 그룹 코드 (CE7 카페, FD6 음식점). 예전 후보는 null
    @Column(length = 10)
    private String categoryCode;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    // 후보 유형: FASTEST(빠른) / BALANCED(균형) / OPTIMAL(최적)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CandidateType type;

    @Column(nullable = false)
    private Double avgDistanceMeters;

    @Column(nullable = false)
    private Double maxDistanceMeters;

    @Column
    private Integer avgTravelMinutes;

    @Column
    private Integer maxTravelMinutes;

    // 후보 유형과 계산된 거리 또는 이동 시간에 따른 기본 설명.
    @Column(columnDefinition = "TEXT")
    private String description;

    // 이 모임의 이동 시간과 누적 장소 리뷰를 함께 사용한 설명.
    @Column(columnDefinition = "TEXT")
    private String meetingDescription;

    @Builder
    public PlaceCandidate(Meeting meeting, String kakaoPlaceId, String placeName, String address, String categoryCode ,Double latitude, Double longitude,
                           CandidateType type, Double avgDistanceMeters, Double maxDistanceMeters, Integer avgTravelMinutes, Integer maxTravelMinutes, String description) {
        this.meeting = meeting;
        this.kakaoPlaceId = kakaoPlaceId;
        this.placeName = placeName;
        this.address = address;
        this.categoryCode = categoryCode;
        this.latitude = latitude;
        this.longitude = longitude;
        this.type = type;
        this.avgDistanceMeters = avgDistanceMeters;
        this.maxDistanceMeters = maxDistanceMeters;
        this.avgTravelMinutes = avgTravelMinutes;
        this.maxTravelMinutes = maxTravelMinutes;
        this.description = description;
    }

    public enum CandidateType {
        FASTEST,
        BALANCED,
        OPTIMAL
    }
}
