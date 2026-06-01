package com.w2m.backend.availability.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.w2m.backend.availability.entity.Availability;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class AvailabilityResponse {

    private Long id;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startDateTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endDateTime;

    public static AvailabilityResponse from(Availability availability) {
        return AvailabilityResponse.builder()
                .id(availability.getId())
                .startDateTime(availability.getStartDateTime())
                .endDateTime(availability.getEndDateTime())
                .build();
    }
}
