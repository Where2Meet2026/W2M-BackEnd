package com.w2m.backend.participant.repository;

import com.w2m.backend.participant.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParticipantRepository extends JpaRepository<Participant,Long> {
    boolean existsByMeetingIdAndUserId(Long meetingId, Long userId); // 특정 유저가 그 모임에 참여했는지 확인
    List<Participant> findByMeetingId(Long meetingId); // 특정 모임 참여자 목록 조회
    List<Participant> findByUserId(Long userId); // 특정 유저가 참여한 모임 목록 조회할 때 사용
    Optional<Participant> findByMeetingIdAndUserId(Long meetingId, Long userId);
    void deleteByMeetingId(Long meetingId);
}
