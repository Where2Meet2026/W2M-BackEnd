package com.w2m.backend.participant.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.w2m.backend.participant.entity.Participant;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ParticipantResponse {
    private Long participantId;
    private Long meetingId;
    private Long userId;
    private String userName;
    private String role;
    private LocalDateTime joinedAt;

    @JsonProperty("isTimeSelected")
    private boolean isTimeSelected;

    public static ParticipantResponse from(Participant participant) {
        return ParticipantResponse.builder()
                .participantId(participant.getId())
                .meetingId(participant.getMeeting().getId())
                .userId(participant.getUser().getId())
                .userName(participant.getUser().getName())
                .role(participant.getRole().name())
                .joinedAt(participant.getJoinedAt())
                .isTimeSelected(participant.isTimeSelected())
                .build();
    }
}
