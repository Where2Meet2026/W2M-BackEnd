package com.w2m.backend.location.entity;

import com.w2m.backend.participant.entity.Participant;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "participant_locations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ParticipantLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 참여자당 출발 위치는 최대 1건 (unique 제약으로 1:1 보장, 시간 확정 후 1회 입력)
    // 양방향은 Participant.locations 쪽에서 cascade/orphanRemoval 담당
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id", nullable = false, unique = true)
    private Participant participant;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Builder
    public ParticipantLocation(Participant participant, String address, Double latitude, Double longitude) {
        this.participant = participant;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // 등록/수정 구분 없이 덮어쓰는 upsert 방식 (availabilities와 동일 패턴)
    public void update(String address, Double latitude, Double longitude) {
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
