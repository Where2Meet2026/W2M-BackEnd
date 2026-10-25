package com.w2m.backend.review.event;

/**
 * 리뷰가 저장된 뒤 발행된다. 같은 장소의 설명 문구를 다시 만들어야 할지 판단하는 쪽(rag)이 구독한다.
 */
public record ReviewCreatedEvent(String placeId, String placeName) {
}
