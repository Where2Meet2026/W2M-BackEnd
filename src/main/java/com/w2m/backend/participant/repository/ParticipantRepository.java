package com.w2m.backend.participant.repository;

import com.w2m.backend.auth.entity.User;
import com.w2m.backend.participant.entity.Participant;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ParticipantRepository extends JpaRepository<Participant,Long> {
    boolean existsByMeetingIdAndUserId(Long meetingId, Long userId);
    List<Participant> findByMeetingId(Long meetingId);

    // 잠금 조회라 다른 트랜잭션이 방금 커밋한 값까지 읽는다 (전원 위치 입력 완료 판정용)
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select p from Participant p where p.meeting.id = :meetingId")
    List<Participant> findLatestByMeetingId(@Param("meetingId") Long meetingId);

    List<Participant> findByUserId(Long userId); 
    Optional<Participant> findByMeetingIdAndUserId(Long meetingId, Long userId);
    void deleteByMeetingId(Long meetingId);

    Long user(User user);
}
