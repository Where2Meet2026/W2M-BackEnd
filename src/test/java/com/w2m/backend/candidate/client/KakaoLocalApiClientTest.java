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

}
