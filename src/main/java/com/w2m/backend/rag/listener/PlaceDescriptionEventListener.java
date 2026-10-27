package com.w2m.backend.rag.listener;

import com.w2m.backend.rag.service.PlaceDescriptionService;
import com.w2m.backend.review.event.ReviewCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlaceDescriptionEventListener {

    private final PlaceDescriptionService placeDescriptionService;

    /**
     * 리뷰 저장이 커밋된 뒤 별도 스레드에서 실행한다. 리뷰를 쓰는 사람은 LLM 호출을 기다리지 않는다.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReviewCreated(ReviewCreatedEvent event) {
        try {
            placeDescriptionService.regenerate(event.placeId(), event.placeName());
        } catch (Exception e) {
            log.warn("[description-ai] 설명 재생성 실패 placeId={} : {}", event.placeId(), e.toString());
        }
    }
}
