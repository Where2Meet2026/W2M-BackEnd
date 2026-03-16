package com.w2m.backend.meeting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class RoomResponseDto {
    private String roomCode;
    private String title;
    private String inviteLink;
}