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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    @Column(nullable = false)
    private String placeName;

    @Column(nullable = false)
    private String address;

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

    // AI가 리뷰를 바탕으로 생성한 추천 설명 (아직 리뷰가 없으면 null)
    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder
    public PlaceCandidate(Meeting meeting, String placeName, String address, Double latitude, Double longitude,
                           CandidateType type, Double avgDistanceMeters, Double maxDistanceMeters, String description) {
        this.meeting = meeting;
        this.placeName = placeName;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.type = type;
        this.avgDistanceMeters = avgDistanceMeters;
        this.maxDistanceMeters = maxDistanceMeters;
        this.description = description;
    }

    public enum CandidateType {
        FASTEST,
        BALANCED,
        OPTIMAL
    }
}
