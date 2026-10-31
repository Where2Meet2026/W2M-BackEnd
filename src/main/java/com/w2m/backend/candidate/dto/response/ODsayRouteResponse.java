package com.w2m.backend.candidate.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Optional;

// ODsay 대중교통 길찾기 응답. 우리가 쓰는 값(path[0].info)만 받고 나머지 필드는 무시한다.
@JsonIgnoreProperties(ignoreUnknown = true)
public record ODsayRouteResponse(
        Result result
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Result(List<Path> path) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Path(Info info) {}

    // 필드 이름이 JSON 키와 같아서 @JsonProperty 없이도 자동으로 매핑된다.
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Info(
            Integer totalTime,          //총 소요시간(분)
            Integer totalWalk,          // 도보 거리(m)
            Integer busTransitCount,    // 버스 환승 횟수
            Integer subwayTransitCount  // 지하철 환승 횟수
    ) {}

    // 추천 경로(첫 번째)의 정보. 경로가 없거나 응답이 비어 있으면 빈 Optional.
    public Optional<Info> firstRoute() {
        if (result == null || result.path == null ||
        result.path.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(result.path().get(0).info());
    }
}
