package com.w2m.backend.participant.repository;

import com.w2m.backend.participant.entity.Participant;
import com.w2m.backend.meeting.entity.Meeting;
import com.w2m.backend.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {
    List<Participant> findByMeeting(Meeting meeting);
    Optional<Participant> findByMeetingAndUser(Meeting meeting, User user);
    Optional<Participant> findByMeetingAndNickname(Meeting meeting, String nickname);
}
