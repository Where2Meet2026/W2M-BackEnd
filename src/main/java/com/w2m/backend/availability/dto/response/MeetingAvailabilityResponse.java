package com.w2m.backend.availability.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import java.util.List;
import java.util.Map;

/**
 * 모임의 모든 참여자들의 시간대를 통합하여 반환하는 DTO
 * 프론트엔드에서 드래그 뷰(Grid)를 그릴 때 효율적으로 사용할 수 있도록 구성
 */
@Getter
@Builder
@AllArgsConstructor
public class MeetingAvailabilityResponse {
    
    private Long meetingId;
    
    // 닉네임별 가능 시간대 리스트 (Map 형태가 프론트에서 처리하기 빠를 수 있음)
    // key: participantNickname, value: List of time ranges
    private Map<String, List<AvailabilityResponse>> participantAvailabilities;

}
