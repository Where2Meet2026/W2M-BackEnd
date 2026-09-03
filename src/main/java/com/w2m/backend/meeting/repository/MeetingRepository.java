package com.w2m.backend.meeting.repository;

import com.w2m.backend.meeting.entity.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MeetingRepository extends JpaRepository<Meeting,Long> {
    Optional<Meeting> findByInviteCode(String inviteCode);
    List<Meeting> findByHostUserId(Long hostUserId); //내가 만든 방 조회
    List<Meeting> findByIdIn(List<Long> meetingIds);
}
