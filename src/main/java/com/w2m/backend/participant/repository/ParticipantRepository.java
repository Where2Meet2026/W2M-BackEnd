package com.w2m.backend.participant.repository;

import com.w2m.backend.participant.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParticipantRepository extends JpaRepository<Participant,Long> {
    boolean existsByMeetingIdAndUserId(Long meetingId, Long userId); 
    List<Participant> findByMeetingId(Long meetingId); 
    List<Participant> findByUserId(Long userId); 
    Optional<Participant> findByMeetingIdAndUserId(Long meetingId, Long userId);
    void deleteByMeetingId(Long meetingId);
}
