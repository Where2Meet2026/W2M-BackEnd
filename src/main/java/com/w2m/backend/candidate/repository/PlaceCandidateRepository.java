package com.w2m.backend.candidate.repository;

import com.w2m.backend.candidate.entity.PlaceCandidate;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlaceCandidateRepository extends JpaRepository<PlaceCandidate, Long> {
    List<PlaceCandidate> findByMeetingId(Long meetingId);
    void deleteByMeetingId(Long meetingId);

    // 잠금 읽기(FOR SHARE)는 트랜잭션 시작 시점의 스냅샷이 아니라 가장 최근에 커밋된 값을 읽는다.
    // 모임 행 잠금을 기다리는 사이 다른 요청이 만든 후보를 확실히 보려고 쓴다.
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select c from PlaceCandidate c where c.meeting.id = :meetingId")
    List<PlaceCandidate> findLatestByMeetingId(@Param("meetingId") Long meetingId);
}
