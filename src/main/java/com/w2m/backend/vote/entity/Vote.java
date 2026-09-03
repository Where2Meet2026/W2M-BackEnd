package com.w2m.backend.vote.entity;

import com.w2m.backend.candidate.entity.PlaceCandidate;
import com.w2m.backend.participant.entity.Participant;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "votes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private PlaceCandidate candidate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id", nullable = false)
    private Participant participant;

    @Column(nullable = false)
    private boolean liked;

    @Builder
    public Vote(PlaceCandidate candidate, Participant participant, boolean liked) {
        this.candidate = candidate;
        this.participant = participant;
        this.liked = liked;
    }

    public void updateLiked(boolean liked) {
        this.liked = liked;
    }
}
