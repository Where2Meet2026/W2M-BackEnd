package com.w2m.backend.candidate.service;

import com.w2m.backend.candidate.entity.PlaceCandidate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
    @Test
    void 후보3개를생성하고저장한다() {
        List<PlaceCandidate> candidates = placeCandidateService.generateCandidates(20L);

        candidates.forEach(c ->
                System.out.println(c.getType()+ "-" + c.getPlaceName() + "(" +
                        c.getAvgDistanceMeters() + "m)"));

        assertThat(candidates).hasSize(3);
    }
}
