package com.w2m.backend.location.repository;

import com.w2m.backend.location.entity.ParticipantLocation;
import com.w2m.backend.participant.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ParticipantLocationRepository extends JpaRepository<ParticipantLocation, Long> {
    Optional<ParticipantLocation> findByParticipant(Participant participant);

    @Query("SELECT pl FROM ParticipantLocation pl WHERE pl.participant.meeting.id = :meetingId")
    List<ParticipantLocation> findAllByMeetingId(@Param("meetingId") Long meetingId);

    boolean existsByParticipantId(Long participantId);
}
