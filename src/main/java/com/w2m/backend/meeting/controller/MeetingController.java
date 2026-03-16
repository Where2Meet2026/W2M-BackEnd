package com.w2m.backend.meeting.controller;

import com.w2m.backend.meeting.dto.JoinRequestDto;
import com.w2m.backend.meeting.dto.JoinResponseDto;
import com.w2m.backend.meeting.dto.RoomRequestDto;
import com.w2m.backend.meeting.dto.RoomResponseDto;
import com.w2m.backend.meeting.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class MeetingController {
    private final RoomService roomService;

    @PostMapping
    public ResponseEntity<RoomResponseDto> create(@RequestBody RoomRequestDto request) {
        RoomResponseDto response = roomService.createRoom(request.getTitle());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{roomCode}/join")
    public ResponseEntity<JoinResponseDto> joinRoom(
            @PathVariable String roomCode,
            @RequestBody JoinRequestDto requestDto) {

        JoinResponseDto response = roomService.joinRoom(roomCode, requestDto);
        return ResponseEntity.ok(response);
    }
}
