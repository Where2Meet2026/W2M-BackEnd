package com.w2m.backend.candidate.client;

import com.w2m.backend.candidate.dto.response.KakaoLocalSearchResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class KakaoLocalApiClient {

    @Value("${KAKAO_REST_API_KEY}") // 카카오 로그인용 KAKAO_CLIENT_ID 와는 다른 키 (로컬 장소검색 API 전용)
    private String restApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public String searchByKeyword(String keyword) {
        String url = "https://dapi.kakao.com/v2/local/search/keyword.json?query=" + keyword;

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "KakaoAK " + restApiKey);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity,
                String.class);
        return response.getBody();
    }
    public KakaoLocalSearchResponse searchByCategory(double latitude, double longitude, int radiusMeters, String categoryGroupCode) {
        String url = "https://dapi.kakao.com/v2/local/search/category.json?category_group_code=" +
                categoryGroupCode +"&x=" + longitude+"&y=" + latitude + "&radius="+ radiusMeters;

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "KakaoAK " + restApiKey);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<KakaoLocalSearchResponse> response = restTemplate.exchange(url, HttpMethod.GET, entity,
                KakaoLocalSearchResponse.class);
        return response.getBody(); // 카카오가 보내준 거 본문만 받아오기
    }
}
