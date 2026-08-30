package com.w2m.backend.candidate.repository;

import com.w2m.backend.candidate.entity.PlaceCandidate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlaceCandidateRepository extends JpaRepository<PlaceCandidate, Long> {
    List<PlaceCandidate> findByMeetingId(Long meetingId);
}
