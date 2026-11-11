package com.w2m.backend.candidate.entity;

import com.w2m.backend.meeting.entity.Meeting;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 이 모임에서 싫어요 과반으로 후보에서 빠진 장소. 후보를 교체할 때 다시 고르지 않도록 기록해 둔다.
@Entity
@Table(name = "rejected_places", uniqueConstraints = @UniqueConstraint(columnNames = {"meeting_id", "kakao_place_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RejectedPlace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    @Column(name = "kakao_place_id", nullable = false)
    private String kakaoPlaceId;

    @Builder
    public RejectedPlace(Meeting meeting, String kakaoPlaceId) {
        this.meeting = meeting;
        this.kakaoPlaceId = kakaoPlaceId;
    }
}
