package com.w2m.backend.rag.generator;

import java.util.List;
import java.util.Optional;

/**
 * 리뷰 코멘트를 근거로 장소 소개 문구를 만든다. 제공자(Gemini 등)를 바꿔도 서비스 코드는 그대로 두기 위한 경계.
 * 실패하거나 사용할 수 없는 상태면 예외 대신 빈 값을 돌려줘야 한다.
 */
public interface DescriptionGenerator {

    Optional<String> generate(String placeName, double averageRating, int reviewCount, List<String> comments);
}
