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
    private String category;
    private String inviteCode;
    private String status;

    private String role;
    private LocalDateTime confirmedStartDateTime;
    private LocalDateTime confirmedEndDateTime;
    private boolean canDelete;
    private boolean canLeave;
    // 내가 이 모임의 확정 장소에 이미 리뷰를 작성했는지 (내 방 목록에서만 계산하고, 그 외 응답에서는 false)
    private boolean reviewed;

    public static MeetingResponse from(Meeting meeting, String role) {
        return from(meeting, role, false);
    }

    public static MeetingResponse from(Meeting meeting, String role, boolean reviewed) {
        return MeetingResponse.builder()
                .meetingId(meeting.getId())
                .title(meeting.getTitle())
                .description(meeting.getDescription())
                .category(meeting.getCategory() != null ? meeting.getCategory().name() : null)
                .inviteCode(meeting.getInviteCode())
                .status(meeting.getStatus().name())
                .confirmedStartDateTime(meeting.getConfirmedStartDateTime())
                .confirmedEndDateTime(meeting.getConfirmedEndDateTime())
                .role(role)
                .canDelete("HOST".equals(role))
                .canLeave("PARTICIPANT".equals(role))
                .reviewed(reviewed)
                .build();
    }
}
