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
    private Integer avgTravelMinutes;
    // 카카오 장소 ID. 결과 화면 "지도에서 보기"에서 카카오맵의 그 장소 화면(/link/map/장소ID)을 여는 데 쓴다
    private String kakaoPlaceId;
    private LocalDateTime confirmedStartDateTime;
    private LocalDateTime confirmedEndDateTime;

    public static FinalSelectionResponse from (Meeting meeting) {
        PlaceCandidate candidate = meeting.getConfirmedCandidate();
        return FinalSelectionResponse.builder()
                .placeName(candidate.getPlaceName())
                .address(candidate.getAddress())
                .latitude(candidate.getLatitude())
                .longitude(candidate.getLongitude())
                .avgTravelMinutes(candidate.getAvgTravelMinutes())
                .kakaoPlaceId(candidate.getKakaoPlaceId())
                .confirmedStartDateTime(meeting.getConfirmedStartDateTime())
                .confirmedEndDateTime(meeting.getConfirmedEndDateTime())
                .build();
    }
}
