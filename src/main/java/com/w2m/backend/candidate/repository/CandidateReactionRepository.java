package com.w2m.backend.candidate.repository;

import com.w2m.backend.candidate.entity.CandidateReaction;
import com.w2m.backend.candidate.entity.PlaceCandidate;
import com.w2m.backend.participant.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CandidateReactionRepository extends JpaRepository<CandidateReaction, Long> {

    long countByCandidateAndReactionType(PlaceCandidate candidate, CandidateReaction.ReactionType
            reactionType); // 이 후보에 이 타입(LIKE/DISLIKE) 반응이 몇 개인지 셈

        Optional<CandidateReaction> findByCandidateAndParticipant(PlaceCandidate candidate,
  Participant participant); // 이 후보에 이 참여자가 남긴 반응이 있는 찾음
}
