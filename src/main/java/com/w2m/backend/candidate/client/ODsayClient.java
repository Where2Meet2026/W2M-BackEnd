package com.w2m.backend.candidate.client;

import com.w2m.backend.candidate.dto.response.ODsayRouteResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Optional;

@Slf4j
@Component
public class ODsayClient {

    private static final String ROUTE_URL = "https://api.odsay.com/v1/api/searchPubTransPathT";

    private final String apiKey;
    private final RestTemplate restTemplate;

    public ODsayClient(
            @Value("${odsay.api-key:") String apiKey,
            @Value("${odsay.timeout-ms:3000") int timeoutMs) {
        this.apiKey = apiKey;

        //외부 API가 느려져도 요청이 매달리지 않도록 연결/응답 대기 시간에 상한을 둔다
        SimpleClientHttpRequestFactory factory = new
                SimpleClientHttpRequestFactory();
                factory.setConnectTimeout(timeoutMs);
                factory.setReadTimeout(timeoutMs);
                this.restTemplate = new RestTemplate(factory);
    }

    //출발지 -> 도착지 대중교통 추천 경로의 정보. 키가 없거나 호출이 실패하면 빈 Optional(호출한 쪽이 하버사인으로 교체)
    public Optional<ODsayRouteResponse.Info> findRoute(double startLat,
   double startLng, double endLat, double endLng) {
                if (apiKey == null || apiKey.isBlank()) {
                    return Optional.empty();
                }

                URI uri = UriComponentsBuilder.fromUriString(ROUTE_URL)
                        .queryParam("SX", startLng)
                        .queryParam("SY",startLat)
                        .queryParam("EX", endLng)
                        .queryParam("EY", endLat)
                        .queryParam("apiKey", apiKey)
                        .build().toUri();

                try {
                    ODsayRouteResponse response = restTemplate.getForObject(uri,
                            ODsayRouteResponse.class);
                    return response == null ? Optional.empty() :
                            response.firstRoute();
                } catch (RestClientException e) {
                    log.warn("ODsay 호출 실패: {}", e.getClass().getSimpleName());
                    return Optional.empty();
                }
    }
}
