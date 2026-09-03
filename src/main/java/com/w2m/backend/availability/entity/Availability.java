package com.w2m.backend.availability.entity;

import com.w2m.backend.participant.entity.Participant;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "availabilities")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Availability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id", nullable = false)
    private Participant participant;

    @Column(nullable = false)
    private LocalDateTime startDateTime;

    @Column(nullable = false)
    private LocalDateTime endDateTime;

    @Builder
    public Availability(Participant participant, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        this.participant = participant;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
    }
}
