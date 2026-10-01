package com.w2m.backend.location.dto.response;

import com.w2m.backend.location.entity.ParticipantLocation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class LocationResponse {

    private Long participantId;
    private String address;
    private Double latitude;
    private Double longitude;

    public static LocationResponse from(ParticipantLocation location) {
        return LocationResponse.builder()
                .participantId(location.getParticipant().getId())
                .address(location.getAddress())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .build();
    }
}
