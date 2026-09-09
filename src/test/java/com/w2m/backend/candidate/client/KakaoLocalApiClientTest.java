package com.w2m.backend.candidate.client;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
public class KakaoLocalApiClientTest {

    @Autowired
    private KakaoLocalApiClient kakaoLocalApiClient;

    @Test
    void 강남역으로검색하면결과나옴() {
        String result = kakaoLocalApiClient.searchByKeyword("강남역");

        System.out.println(result);
        assertThat(result).isNotNull();
        assertThat(result).contains("documents");
    }
    @Test
    void 중간지점근처음식점을검색하결과가나옴() {
        String result = kakaoLocalApiClient.searchByCategory(37.4979, 127.0276,1000, "FD6");

        System.out.println("결과는 : " + result);
        assertThat(result).isNotNull(); //응답이 비어있으면 실패
        assertThat(result).contains("documents"); // documents라는 단어 없으면 실패
    }

}
