package com.w2m.backend.availability.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 많은 양의 시간 데이터를 효율적으로 받기 위한 DTO
 */
@Getter
@NoArgsConstructor
public class SaveAvailabilityRequest {
    
    @NotEmpty(message = "최소 하나 이상의 시간대를 선택해야 합니다.")
    private List<TimeRangeRequest> timeRanges;

    @Getter
    @NoArgsConstructor
    public static class TimeRangeRequest {
        
        // 프론트엔드와 포맷을 맞추기 위해 명시적으로 지정 (ISO 8601 권장)
        @NotNull(message = "시작 시간은 필수입니다.")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime startDateTime;

        @NotNull(message = "종료 시간은 필수입니다.")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime endDateTime;
    }
}
