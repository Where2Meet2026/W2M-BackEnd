package com.w2m.backend.candidate.repository;

import com.w2m.backend.candidate.entity.RejectedPlace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RejectedPlaceRepository extends JpaRepository<RejectedPlace, Long> {

    List<RejectedPlace> findByMeetingId(Long meetingId);

    void deleteByMeetingId(Long meetingId);
}
