package com.w2m.backend.meeting.dto.response;

import com.w2m.backend.candidate.entity.PlaceCandidate;
import com.w2m.backend.meeting.entity.Meeting;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class FinalSelectionResponse {
    private String placeName;
    private String address;
    private Double latitude;
    private Double longitude;
    private LocalDateTime confirmedStartDateTime;
    private LocalDateTime confirmedEndDateTime;

    public static FinalSelectionResponse from (Meeting meeting) {
        PlaceCandidate candidate = meeting.getConfirmedCandidate();
        return FinalSelectionResponse.builder()
                .placeName(candidate.getPlaceName())
                .address(candidate.getAddress())
                .latitude(candidate.getLatitude())
                .longitude(candidate.getLongitude())
                .confirmedStartDateTime(meeting.getConfirmedStartDateTime())
                .confirmedEndDateTime(meeting.getConfirmedEndDateTime())
                .build();
    }
}
