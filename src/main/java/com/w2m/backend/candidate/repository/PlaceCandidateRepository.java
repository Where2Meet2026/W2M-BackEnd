package com.w2m.backend.candidate.repository;

import com.w2m.backend.candidate.entity.PlaceCandidate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlaceCandidateRepository extends JpaRepository<PlaceCandidate, Long> {
    List<PlaceCandidate> findByMeetingId(Long meetingId);
    void deleteByMeetingId(Long meetingId);
    // 이 모임에 후보가 하나라도 만들어졌는지.
    // 후보가 만들어진 뒤에는 참여자 나가기를 막는 데 쓴다 (ParticipantService.leaveMeeting, 내 방 목록의 canLeave)
    boolean existsByMeetingId(Long meetingId);
}
