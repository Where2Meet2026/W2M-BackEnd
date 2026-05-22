package com.w2m.backend.meeting.dto.response;

import com.w2m.backend.meeting.entity.Meeting;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MeetingResponse {

    private Long meetingId;
    private String title;
    private String description;
    private String inviteCode;
    private String status;

    public static MeetingResponse from(Meeting meeting) {
        return MeetingResponse.builder()
                .meetingId(meeting.getId())
                .title(meeting.getTitle())
                .description(meeting.getDescription())
                .inviteCode(meeting.getInviteCode())
                .status(meeting.getStatus().name())
                .build();
    }
}
