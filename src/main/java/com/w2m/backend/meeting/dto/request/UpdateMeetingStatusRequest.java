package com.w2m.backend.meeting.dto.request;

import com.w2m.backend.meeting.entity.Meeting;
import lombok.Getter;

@Getter
public class UpdateMeetingStatusRequest {
    private Meeting.MeetingStatus status;
}
