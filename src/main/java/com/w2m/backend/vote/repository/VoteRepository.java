package com.w2m.backend.vote.repository;

import com.w2m.backend.vote.entity.Vote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VoteRepository extends JpaRepository<Vote, Long> {
    List<Vote> findByCandidateId(Long candidateId); // 이 후보가 받은 투표 전체 — 후보별 득표수 셀 때 씀

    Optional<Vote> findByMeetingIdAndParticipantId(Long meetingId, Long participantId); // 이 참여자가 이 모임에서 이미 투표했는지 확인용

    long countByMeetingId(Long meetingId); // 이 모임에서 지금까지 몇 명 투표했는지 확인용
    List<Vote> findByMeetingId(Long meetingId);
    void deleteByMeetingId(Long meetingId);
    void deleteByParticipantId(Long participantId);
    void deleteByCandidateId(Long candidateId);
}
