package com.w2m.backend.rag.service;

import com.w2m.backend.candidate.entity.PlaceCandidate;

import java.util.Locale;

/**
 * 리뷰가 부족해 LLM 설명을 쓸 수 없을 때의 기본 문구. LLM 없이 값만 끼워 넣는다.
 * 예) "평균 265m · 가장 빠른 동선", "평균 1.2km · 종합 추천"
 */
public final class DefaultDescriptionFormatter {

    private DefaultDescriptionFormatter() {
    }

    public static String format(PlaceCandidate.CandidateType type, double avgDistanceMeters) {
        return "평균 " + formatDistance(avgDistanceMeters) + " · " + label(type);
    }

    // 미터를 정수로 반올림한 뒤, 1000m 이상이면 소수 첫째 자리 km로 표기한다.
    static String formatDistance(double meters) {
        long rounded = Math.round(meters);
        if (rounded >= 1000) {
            return String.format(Locale.ROOT, "%.1fkm", rounded / 1000.0);
        }
        return rounded + "m";
    }

    static String label(PlaceCandidate.CandidateType type) {
        return switch (type) {
            case FASTEST -> "가장 빠른 동선";
            case BALANCED -> "이동 부담이 고른 곳";
            case OPTIMAL -> "종합 추천";
        };
    }
}
