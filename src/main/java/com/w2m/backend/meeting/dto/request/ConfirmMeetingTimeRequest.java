package com.w2m.backend.meeting.dto.request;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ConfirmMeetingTimeRequest {
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;

}
