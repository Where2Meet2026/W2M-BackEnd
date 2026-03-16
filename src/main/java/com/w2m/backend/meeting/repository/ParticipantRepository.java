package com.w2m.backend.meeting.repository;

import com.w2m.backend.meeting.entity.Participant;
import com.w2m.backend.meeting.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {
    Optional<Participant> findByRoomAndNickname(Room room, String nickname);
}
