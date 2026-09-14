package com.w2m.backend.meeting.dto.request;

import com.w2m.backend.meeting.entity.Meeting;
import lombok.Getter;

@Getter
public class CreateMeetingRequest {

    private String title;
    private String description;
    private Meeting.PlaceCategory category;

}
