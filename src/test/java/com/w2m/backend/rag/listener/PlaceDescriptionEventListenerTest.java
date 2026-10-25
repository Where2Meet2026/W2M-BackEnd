package com.w2m.backend.rag.listener;

import com.w2m.backend.rag.service.PlaceDescriptionService;
import com.w2m.backend.review.event.ReviewCreatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PlaceDescriptionEventListenerTest {

    @Mock private PlaceDescriptionService placeDescriptionService;

    @InjectMocks
    private PlaceDescriptionEventListener listener;

    @Test
    void 리뷰_저장_이벤트를_받으면_해당_장소의_설명_재생성을_요청한다() {
        listener.onReviewCreated(new ReviewCreatedEvent("kakao-1", "감성 루프탑 카페"));

        verify(placeDescriptionService).regenerate("kakao-1", "감성 루프탑 카페");
    }

    @Test
    void 재생성이_실패해도_예외를_밖으로_던지지_않는다() {
        doThrow(new RuntimeException("boom")).when(placeDescriptionService).regenerate("kakao-1", "카페");

        assertThatCode(() -> listener.onReviewCreated(new ReviewCreatedEvent("kakao-1", "카페")))
                .doesNotThrowAnyException();
    }
}
