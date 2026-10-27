package com.w2m.backend.rag.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 장소(place_id)별로 LLM이 리뷰를 근거로 만든 설명 문구의 캐시.
 * reviewCount 는 생성 당시 사용한 코멘트 개수로, 이 값이 바뀌었을 때만 다시 생성한다.
 */
@Entity
@Table(
        name = "place_descriptions",
        uniqueConstraints = @UniqueConstraint(name = "uk_place_description_place", columnNames = "place_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceDescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "place_id", nullable = false)
    private String placeId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "review_count", nullable = false)
    private int reviewCount;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @Builder
    public PlaceDescription(String placeId, String description, int reviewCount) {
        this.placeId = placeId;
        this.description = description;
        this.reviewCount = reviewCount;
        this.generatedAt = LocalDateTime.now();
    }

    public void update(String description, int reviewCount) {
        this.description = description;
        this.reviewCount = reviewCount;
        this.generatedAt = LocalDateTime.now();
    }
}
