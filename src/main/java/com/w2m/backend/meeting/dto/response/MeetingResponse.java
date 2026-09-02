package com.w2m.backend.meeting.dto.response;

import com.w2m.backend.meeting.entity.Meeting;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MeetingResponse {

    private Long meetingId;
    private String title;
    private String description;
    private String inviteCode;
    private String status;

    private String role;
    private LocalDateTime confirmedStartDateTime;
    private LocalDateTime confirmedEndDateTime;
    private boolean canDelete;
    private boolean canLeave;

    public static MeetingResponse from(Meeting meeting, String role) {
        return MeetingResponse.builder()
                .meetingId(meeting.getId())
                .title(meeting.getTitle())
                .description(meeting.getDescription())
                .inviteCode(meeting.getInviteCode())
                .status(meeting.getStatus().name())
                .confirmedStartDateTime(meeting.getConfirmedStartDateTime())
                .confirmedEndDateTime(meeting.getConfirmedEndDateTime())
                .role(role)
                .canDelete("HOST".equals(role))
                .canLeave("PARTICIPANT".equals(role))
                .build();
    }
}
