package com.w2m.backend.meeting.dto.request;

import com.w2m.backend.meeting.entity.Meeting;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class CreateMeetingRequest {

    // 길이 제한은 meetings 테이블 컬럼 길이(title 100, description 500)와 맞춘다. 넘기면 DB 오류(500)가 난다.
    @NotBlank(message = "모임 이름을 입력해주세요.")
    @Size(max = 100, message = "모임 이름은 100자 이하로 입력해주세요.")
    private String title;

    @Size(max = 500, message = "모임 설명은 500자 이하로 입력해주세요.")
    private String description;

    private Meeting.PlaceCategory category;

}
