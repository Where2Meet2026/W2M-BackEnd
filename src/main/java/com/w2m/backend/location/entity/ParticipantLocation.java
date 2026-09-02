package com.w2m.backend.location.entity;

import com.w2m.backend.participant.entity.Participant;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "participant_locations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ParticipantLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id", nullable = false)
    private Participant participant;

    // 이 위치가 적용되는 날짜 (참여자가 날짜별로 다른 출발 위치를 등록할 수 있음)
    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Builder
    public ParticipantLocation(Participant participant, LocalDate date, String address, Double latitude, Double longitude) {
        this.participant = participant;
        this.date = date;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
