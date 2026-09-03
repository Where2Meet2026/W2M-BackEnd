package com.w2m.backend.vote.repository;

import com.w2m.backend.vote.entity.Vote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VoteRepository extends JpaRepository<Vote, Long> {
    List<Vote> findByCandidateId(Long candidateId);
    Optional<Vote> findByCandidateIdAndParticipantId(Long candidateId, Long participantId);
    long countByCandidateIdAndLiked(Long candidateId, boolean liked);
}
