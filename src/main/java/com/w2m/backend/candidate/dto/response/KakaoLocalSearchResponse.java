package com.w2m.backend.candidate.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true) // 카카오응답엔 우리가 안 쓰는 필드가 많아서 무시하도록 설정
public record KakaoLocalSearchResponse(
        List<KakaoPlaceDto> documents
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KakaoPlaceDto (
            @JsonProperty("id") String placeId,
            @JsonProperty("place_name") String placeName,
            @JsonProperty("address_name") String addressName,
            @JsonProperty("category_name") String categoryName,
            @JsonProperty("x") String longitude,
            @JsonProperty("y") String latitude
    ) {}
}
