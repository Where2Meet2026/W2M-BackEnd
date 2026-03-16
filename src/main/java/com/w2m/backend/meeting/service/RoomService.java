package com.w2m.backend.meeting.service;

import com.w2m.backend.meeting.dto.JoinRequestDto;
import com.w2m.backend.meeting.dto.JoinResponseDto;
import com.w2m.backend.meeting.dto.RoomResponseDto;
import com.w2m.backend.meeting.entity.Participant;
import com.w2m.backend.meeting.entity.Room;
import com.w2m.backend.meeting.repository.ParticipantRepository;
import com.w2m.backend.meeting.repository.RoomRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoomService {
    private final RoomRepository roomRepository;
    private final ParticipantRepository participantRepository;

    @Transactional
    public RoomResponseDto createRoom(String title) {
        // 1. 중복 없는 랜덤 코드 생성 (예: 8자리)
        String roomCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // 2. 방 객체 생성 및 저장
        Room room = Room.builder()
                .roomCode(roomCode)
                .title(title)
                .build();

        Room savedRoom = roomRepository.save(room);

        // 3. 응답 데이터 반환 (링크 포함)
        String inviteLink = "https://where2meet.com/rooms/" + roomCode;
        return new RoomResponseDto(savedRoom.getRoomCode(), savedRoom.getTitle(), inviteLink);
    }

    /**
     * 기존 방 입장 및 재접속 로직
     */
    @Transactional
    public JoinResponseDto joinRoom(String roomCode, JoinRequestDto dto) {
        // 1. 방 존재 여부 확인
        Room room = roomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 방입니다."));

        // 2. 만료 여부 확인
        if (room.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("만료된 링크입니다.");
        }

        // 3. 닉네임 중복 확인 및 재접속 처리
        Optional<Participant> existingMember =
                participantRepository.findByRoomAndNickname(room, dto.getNickname());

        if (existingMember.isPresent()) {
            // 이미 있는 이름이라면 비밀번호 검증 (재접속)
            if (existingMember.get().getPassword().equals(dto.getPassword())) {
                return JoinResponseDto.builder()
                        .roomCode(roomCode)
                        .nickname(dto.getNickname())
                        .message("재접속에 성공했습니다.")
                        .build();
            } else {
                throw new RuntimeException("비밀번호가 일치하지 않거나 이미 사용 중인 닉네임입니다.");
            }
        }

        // 4. 새로운 참여자 등록
        return joinProcess(room, dto.getNickname(), dto.getPassword(), "신규 입장 완료");
    }

    private JoinResponseDto joinProcess(Room room, String nickname, String password, String message) {
        Participant participant = Participant.builder()
                .room(room)
                .nickname(nickname)
                .password(password)
                .joinedAt(LocalDateTime.now())
                .build();
        participantRepository.save(participant);

        return JoinResponseDto.builder()
                .roomCode(room.getRoomCode())
                .nickname(nickname)
                .message(message)
                .build();
    }
}
