package com.w2m.backend.rag.service;

import com.w2m.backend.candidate.entity.PlaceCandidate.CandidateType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultDescriptionFormatterTest {

    @Test
    void 가장_빠른_동선_기본_문구_형식() {
        assertThat(DefaultDescriptionFormatter.format(CandidateType.FASTEST, 265.4))
                .isEqualTo("평균 265m · 가장 빠른 동선");
    }

    @Test
    void 유형별_라벨이_다르다() {
        assertThat(DefaultDescriptionFormatter.format(CandidateType.BALANCED, 310))
                .isEqualTo("평균 310m · 이동 부담이 고른 곳");
        assertThat(DefaultDescriptionFormatter.format(CandidateType.OPTIMAL, 330))
                .isEqualTo("평균 330m · 종합 추천");
    }

    @Test
    void 미터는_정수로_반올림한다() {
        assertThat(DefaultDescriptionFormatter.formatDistance(264.5)).isEqualTo("265m");
        assertThat(DefaultDescriptionFormatter.formatDistance(264.4)).isEqualTo("264m");
        assertThat(DefaultDescriptionFormatter.formatDistance(0)).isEqualTo("0m");
    }

    @Test
    void 천_미터_미만은_m로_표기한다() {
        assertThat(DefaultDescriptionFormatter.formatDistance(999.4)).isEqualTo("999m");
    }

    @Test
    void 천_미터_이상은_소수_첫째_자리_km로_표기한다() {
        assertThat(DefaultDescriptionFormatter.formatDistance(1000)).isEqualTo("1.0km");
        assertThat(DefaultDescriptionFormatter.formatDistance(1234)).isEqualTo("1.2km");
        assertThat(DefaultDescriptionFormatter.formatDistance(2000)).isEqualTo("2.0km");
    }

    @Test
    void 반올림하면_천_미터가_되는_값은_km로_표기한다() {
        assertThat(DefaultDescriptionFormatter.formatDistance(999.6)).isEqualTo("1.0km");
    }

    @Test
    void km_기본_문구_예시() {
        assertThat(DefaultDescriptionFormatter.format(CandidateType.OPTIMAL, 1234))
                .isEqualTo("평균 1.2km · 종합 추천");
    }
}
