package com.w2m.backend.meeting.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class JoinRequestDto {
    private String title;
    private String nickname;
    private String password;
}
