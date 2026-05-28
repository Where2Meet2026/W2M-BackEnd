package com.w2m.backend.availability.repository;

import com.w2m.backend.availability.entity.Availability;
import com.w2m.backend.participant.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AvailabilityRepository extends JpaRepository<Availability, Long> {
    List<Availability> findByParticipant(Participant participant);
    void deleteByParticipant(Participant participant);
}
