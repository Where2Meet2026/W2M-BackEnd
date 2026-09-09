package com.w2m.backend.candidate.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
public class PlaceCandidateServiceTest {

    @Autowired
    private PlaceCandidateService placeCandidateService;

    @Test
    void 참여자가한명이그사람위치가그대로중간지점(){
        PlaceCandidateService.Coordinate centroid = placeCandidateService.calculateCentroid(20L);

        System.out.println("중간지점은 "+ centroid + "입니다!");
        assertThat(centroid.latitude()).isEqualTo(37.4979);
        assertThat(centroid.longitude()).isEqualTo(127.0276);
    }
}
