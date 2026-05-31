package com.w2m.backend.availability.repository;

import com.w2m.backend.availability.entity.Availability;
import com.w2m.backend.participant.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AvailabilityRepository extends JpaRepository<Availability, Long> {
    List<Availability> findByParticipant(Participant participant);
    void deleteByParticipant(Participant participant);
    
    @Query("SELECT COUNT(a) > 0 FROM Availability a WHERE a.participant.id = :participantId")
    boolean existsByParticipantId(@Param("participantId") Long participantId);
    
    void deleteByParticipantId(Long participantId);
}
