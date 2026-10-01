package com.w2m.backend.meeting.repository;

import com.w2m.backend.meeting.entity.Meeting;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface MeetingRepository extends JpaRepository<Meeting,Long> {
    Optional<Meeting> findByInviteCode(String inviteCode);
    List<Meeting> findByHostUserId(Long hostUserId); //내가 만든 방 조회
    List<Meeting> findByIdIn(List<Long> meetingIds);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Meeting> findWithLockById(Long id); // 투표 저장 중 다른 투표 요청이 끼어들지 못하게 이 모임 행을 잠그고 조회
}
