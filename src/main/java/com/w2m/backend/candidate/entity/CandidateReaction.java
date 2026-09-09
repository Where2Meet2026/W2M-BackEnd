package com.w2m.backend.candidate.entity;

import com.w2m.backend.participant.entity.Participant;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "candidate_reactions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CandidateReaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private PlaceCandidate candidate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name ="participant_id",nullable = false)
    private Participant participant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReactionType reactionType;

    @Builder
    public CandidateReaction(PlaceCandidate candidate, Participant participant, ReactionType reactionType) {
        this.candidate = candidate;
        this.participant = participant;
        this.reactionType = reactionType;
    }
    public enum ReactionType {
        LIKE,
        DISLIKE
    }
}
