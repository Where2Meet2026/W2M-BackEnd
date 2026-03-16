package com.w2m.backend.meeting.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class JoinResponseDto {
    private String roomCode;
    private String nickname;
    private String message;

}