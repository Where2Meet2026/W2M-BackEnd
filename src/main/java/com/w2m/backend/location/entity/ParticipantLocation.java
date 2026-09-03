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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id", nullable = false)
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
}
