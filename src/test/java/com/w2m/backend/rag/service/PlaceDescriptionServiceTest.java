package com.w2m.backend.rag.service;

import com.w2m.backend.rag.entity.PlaceDescription;
import com.w2m.backend.rag.generator.DescriptionGenerator;
import com.w2m.backend.rag.repository.PlaceDescriptionRepository;
import com.w2m.backend.review.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// DB/LLM 없이 레포지토리와 생성기를 mock으로 대체하는 순수 단위 테스트.
@ExtendWith(MockitoExtension.class)
class PlaceDescriptionServiceTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private PlaceDescriptionRepository placeDescriptionRepository;
    @Mock private DescriptionGenerator descriptionGenerator;

    private PlaceDescriptionService service;

    private static final String PLACE_ID = "kakao-1";
    private static final String PLACE_NAME = "감성 루프탑 카페";
    private static final List<String> COMMENTS = List.of("조용해요", "분위기 좋아요");

    @BeforeEach
    void setUp() {
        service = new PlaceDescriptionService(reviewRepository, placeDescriptionRepository, descriptionGenerator, 5, 20);
    }

    private PlaceDescription saved(String description, int reviewCount) {
        return PlaceDescription.builder().placeId(PLACE_ID).description(description).reviewCount(reviewCount).build();
    }

    @Test
    void 조회_코멘트가_최소_개수_미만이면_빈_값이고_LLM을_부르지_않는다() {
        when(reviewRepository.countUsableByPlaceId(PLACE_ID)).thenReturn(4L);

        assertThat(service.resolve(PLACE_ID, PLACE_NAME)).isEmpty();

        verify(descriptionGenerator, never()).generate(anyString(), anyDouble(), anyInt(), anyList());
        verify(placeDescriptionRepository, never()).findByPlaceId(anyString());
    }

    @Test
    void 조회_저장된_설명이_있으면_그대로_돌려주고_LLM을_부르지_않는다() {
        when(reviewRepository.countUsableByPlaceId(PLACE_ID)).thenReturn(8L);
        // 리뷰 수가 달라졌어도(5 -> 8) 조회 경로에서는 기다리지 않고 저장된 문구를 쓴다.
        when(placeDescriptionRepository.findByPlaceId(PLACE_ID)).thenReturn(Optional.of(saved("저장된 설명", 5)));

        assertThat(service.resolve(PLACE_ID, PLACE_NAME)).contains("저장된 설명");

        verify(descriptionGenerator, never()).generate(anyString(), anyDouble(), anyInt(), anyList());
    }

    @Test
    void 조회_코멘트는_충분한데_저장된_설명이_없으면_생성해서_저장한다() {
        when(reviewRepository.countUsableByPlaceId(PLACE_ID)).thenReturn(7L);
        when(placeDescriptionRepository.findByPlaceId(PLACE_ID)).thenReturn(Optional.empty());
        when(reviewRepository.findUsableComments(eq(PLACE_ID), any(Pageable.class))).thenReturn(COMMENTS);
        when(reviewRepository.averageRatingByPlaceId(PLACE_ID)).thenReturn(4.2);
        when(descriptionGenerator.generate(PLACE_NAME, 4.2, 7, COMMENTS)).thenReturn(Optional.of("조용하다는 평이 많아요."));

        assertThat(service.resolve(PLACE_ID, PLACE_NAME)).contains("조용하다는 평이 많아요.");

        ArgumentCaptor<PlaceDescription> captor = ArgumentCaptor.forClass(PlaceDescription.class);
        verify(placeDescriptionRepository).save(captor.capture());
        assertThat(captor.getValue().getPlaceId()).isEqualTo(PLACE_ID);
        assertThat(captor.getValue().getDescription()).isEqualTo("조용하다는 평이 많아요.");
        assertThat(captor.getValue().getReviewCount()).isEqualTo(7);
    }

    @Test
    void 생성할_때_최신_코멘트를_최대_개수만큼만_가져온다() {
        when(reviewRepository.countUsableByPlaceId(PLACE_ID)).thenReturn(30L);
        when(placeDescriptionRepository.findByPlaceId(PLACE_ID)).thenReturn(Optional.empty());
        when(reviewRepository.findUsableComments(eq(PLACE_ID), any(Pageable.class))).thenReturn(COMMENTS);
        when(reviewRepository.averageRatingByPlaceId(PLACE_ID)).thenReturn(4.0);
        when(descriptionGenerator.generate(anyString(), anyDouble(), anyInt(), anyList())).thenReturn(Optional.empty());

        service.resolve(PLACE_ID, PLACE_NAME);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(reviewRepository).findUsableComments(eq(PLACE_ID), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    void 생성기가_빈_값을_주면_저장하지_않고_빈_값을_돌려준다() {
        when(reviewRepository.countUsableByPlaceId(PLACE_ID)).thenReturn(7L);
        when(placeDescriptionRepository.findByPlaceId(PLACE_ID)).thenReturn(Optional.empty());
        when(reviewRepository.findUsableComments(eq(PLACE_ID), any(Pageable.class))).thenReturn(COMMENTS);
        when(reviewRepository.averageRatingByPlaceId(PLACE_ID)).thenReturn(4.2);
        when(descriptionGenerator.generate(anyString(), anyDouble(), anyInt(), anyList())).thenReturn(Optional.empty());

        assertThat(service.resolve(PLACE_ID, PLACE_NAME)).isEmpty();

        verify(placeDescriptionRepository, never()).save(any());
    }

    @Test
    void 평균_평점이_없으면_0으로_넘긴다() {
        when(reviewRepository.countUsableByPlaceId(PLACE_ID)).thenReturn(7L);
        when(placeDescriptionRepository.findByPlaceId(PLACE_ID)).thenReturn(Optional.empty());
        when(reviewRepository.findUsableComments(eq(PLACE_ID), any(Pageable.class))).thenReturn(COMMENTS);
        when(reviewRepository.averageRatingByPlaceId(PLACE_ID)).thenReturn(null);
        when(descriptionGenerator.generate(PLACE_NAME, 0.0, 7, COMMENTS)).thenReturn(Optional.of("설명"));

        assertThat(service.resolve(PLACE_ID, PLACE_NAME)).contains("설명");
    }

    @Test
    void 동시에_생성해서_저장이_충돌해도_생성한_문구는_돌려준다() {
        when(reviewRepository.countUsableByPlaceId(PLACE_ID)).thenReturn(7L);
        when(placeDescriptionRepository.findByPlaceId(PLACE_ID)).thenReturn(Optional.empty());
        when(reviewRepository.findUsableComments(eq(PLACE_ID), any(Pageable.class))).thenReturn(COMMENTS);
        when(reviewRepository.averageRatingByPlaceId(PLACE_ID)).thenReturn(4.2);
        when(descriptionGenerator.generate(PLACE_NAME, 4.2, 7, COMMENTS)).thenReturn(Optional.of("설명"));
        when(placeDescriptionRepository.save(any(PlaceDescription.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThat(service.resolve(PLACE_ID, PLACE_NAME)).contains("설명");
    }

    @Test
    void 재생성_코멘트가_최소_개수_미만이면_아무것도_하지_않는다() {
        when(reviewRepository.countUsableByPlaceId(PLACE_ID)).thenReturn(3L);

        service.regenerate(PLACE_ID, PLACE_NAME);

        verify(descriptionGenerator, never()).generate(anyString(), anyDouble(), anyInt(), anyList());
        verify(placeDescriptionRepository, never()).save(any());
    }

    @Test
    void 재생성_코멘트_개수가_그대로면_LLM을_부르지_않는다() {
        when(reviewRepository.countUsableByPlaceId(PLACE_ID)).thenReturn(6L);
        when(placeDescriptionRepository.findByPlaceId(PLACE_ID)).thenReturn(Optional.of(saved("기존 설명", 6)));

        service.regenerate(PLACE_ID, PLACE_NAME);

        verify(descriptionGenerator, never()).generate(anyString(), anyDouble(), anyInt(), anyList());
    }

    @Test
    void 재생성_코멘트_개수가_달라지면_다시_만들어_기존_설명을_갱신한다() {
        PlaceDescription existing = saved("기존 설명", 5);
        when(reviewRepository.countUsableByPlaceId(PLACE_ID)).thenReturn(6L);
        when(placeDescriptionRepository.findByPlaceId(PLACE_ID)).thenReturn(Optional.of(existing));
        when(reviewRepository.findUsableComments(eq(PLACE_ID), any(Pageable.class))).thenReturn(COMMENTS);
        when(reviewRepository.averageRatingByPlaceId(PLACE_ID)).thenReturn(4.5);
        when(descriptionGenerator.generate(PLACE_NAME, 4.5, 6, COMMENTS)).thenReturn(Optional.of("새 설명"));

        service.regenerate(PLACE_ID, PLACE_NAME);

        assertThat(existing.getDescription()).isEqualTo("새 설명");
        assertThat(existing.getReviewCount()).isEqualTo(6);
        verify(placeDescriptionRepository).save(existing);
    }

    @Test
    void 재생성_저장된_설명이_없으면_새로_만든다() {
        when(reviewRepository.countUsableByPlaceId(PLACE_ID)).thenReturn(5L);
        when(placeDescriptionRepository.findByPlaceId(PLACE_ID)).thenReturn(Optional.empty());
        when(reviewRepository.findUsableComments(eq(PLACE_ID), any(Pageable.class))).thenReturn(COMMENTS);
        when(reviewRepository.averageRatingByPlaceId(PLACE_ID)).thenReturn(4.0);
        when(descriptionGenerator.generate(PLACE_NAME, 4.0, 5, COMMENTS)).thenReturn(Optional.of("첫 설명"));

        service.regenerate(PLACE_ID, PLACE_NAME);

        ArgumentCaptor<PlaceDescription> captor = ArgumentCaptor.forClass(PlaceDescription.class);
        verify(placeDescriptionRepository).save(captor.capture());
        assertThat(captor.getValue().getDescription()).isEqualTo("첫 설명");
        assertThat(captor.getValue().getReviewCount()).isEqualTo(5);
    }

    @Test
    void 캐시_조회는_저장된_장소의_설명만_맵으로_돌려주고_LLM을_부르지_않는다() {
        when(placeDescriptionRepository.findByPlaceIdIn(List.of("a", "b")))
                .thenReturn(List.of(PlaceDescription.builder().placeId("a").description("A 설명").reviewCount(5).build()));

        assertThat(service.findCached(List.of("a", "b"))).containsOnly(java.util.Map.entry("a", "A 설명"));

        verify(descriptionGenerator, never()).generate(anyString(), anyDouble(), anyInt(), anyList());
        verify(reviewRepository, never()).countUsableByPlaceId(anyString());
    }

    @Test
    void 캐시_조회에_장소가_없으면_저장소를_부르지_않고_빈_맵이다() {
        assertThat(service.findCached(List.of())).isEmpty();
        assertThat(service.findCached(null)).isEmpty();

        verify(placeDescriptionRepository, never()).findByPlaceIdIn(any());
    }
}
