package com.w2m.backend.candidate.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class DistanceCalculatorTest {

    @Test
    void 서울역에서강남역까지거리는대략8키로() {
        double distance = DistanceCalculator.calculateDistance(
                37.5547, 126.9707,
                37.4979, 127.0276
        );
        System.out.println("서울역에서 강남역까지 거리는 " + distance +"입니다.");
        assertThat(distance).isBetween(7000.0, 10000.0);
    }
}
