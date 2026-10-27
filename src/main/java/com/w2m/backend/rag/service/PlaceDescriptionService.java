package com.w2m.backend.rag.service;

import com.w2m.backend.rag.entity.PlaceDescription;
import com.w2m.backend.rag.generator.DescriptionGenerator;
import com.w2m.backend.rag.repository.PlaceDescriptionRepository;
import com.w2m.backend.review.repository.ReviewRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 장소에 쌓인 리뷰(retrieval)를 근거로 LLM 설명(generation)을 만들고 캐시한다.
 * 이 클래스는 일부러 @Transactional 을 쓰지 않는다. LLM 호출이 끝날 때까지 DB 트랜잭션을 붙잡지 않기 위해서다.
 */
@Slf4j
@Service
public class PlaceDescriptionService {

    private final ReviewRepository reviewRepository;
    private final PlaceDescriptionRepository placeDescriptionRepository;
    private final DescriptionGenerator descriptionGenerator;
    private final int minReviews;
    private final int maxReviews;

    @Autowired
    public PlaceDescriptionService(
            ReviewRepository reviewRepository,
            PlaceDescriptionRepository placeDescriptionRepository,
            DescriptionGenerator descriptionGenerator,
            @Value("${description-ai.min-reviews:5}") int minReviews,
            @Value("${description-ai.max-reviews:20}") int maxReviews) {
        this.reviewRepository = reviewRepository;
        this.placeDescriptionRepository = placeDescriptionRepository;
        this.descriptionGenerator = descriptionGenerator;
        this.minReviews = minReviews;
        this.maxReviews = maxReviews;
    }

    /**
     * 후보 설명 조회용. 저장된 LLM 설명이 있으면 바로 돌려주고(오래됐어도 기다리지 않는다),
     * 코멘트는 충분한데 저장된 게 없을 때만 그 자리에서 한 번 생성한다.
     * 비어 있으면 호출한 쪽이 기본 문구를 쓰면 된다.
     */
    public Optional<String> resolve(String placeId, String placeName) {
        long usable = reviewRepository.countUsableByPlaceId(placeId);
        if (usable < minReviews) {
            return Optional.empty();
        }

        Optional<PlaceDescription> saved = placeDescriptionRepository.findByPlaceId(placeId);
        if (saved.isPresent()) {
            return saved.map(PlaceDescription::getDescription);
        }
        return generateAndSave(placeId, placeName, (int) usable);
    }

    /**
     * 리뷰가 새로 저장된 뒤 백그라운드에서 호출한다.
     * 코멘트가 충분하고, 마지막 생성 때보다 개수가 달라졌을 때만 다시 만든다.
     */
    public void regenerate(String placeId, String placeName) {
        long usable = reviewRepository.countUsableByPlaceId(placeId);
        if (usable < minReviews) {
            return;
        }

        Optional<PlaceDescription> saved = placeDescriptionRepository.findByPlaceId(placeId);
        if (saved.isPresent() && saved.get().getReviewCount() == usable) {
            return;
        }
        generateAndSave(placeId, placeName, (int) usable);
    }

    private Optional<String> generateAndSave(String placeId, String placeName, int usableCount) {
        List<String> comments = reviewRepository.findUsableComments(placeId, PageRequest.of(0, maxReviews));
        Double average = reviewRepository.averageRatingByPlaceId(placeId);

        Optional<String> generated = descriptionGenerator.generate(
                placeName, average == null ? 0.0 : average, usableCount, comments);
        generated.ifPresent(text -> save(placeId, text, usableCount));
        return generated;
    }

    private void save(String placeId, String text, int usableCount) {
        try {
            placeDescriptionRepository.findByPlaceId(placeId).ifPresentOrElse(
                    existing -> {
                        existing.update(text, usableCount);
                        placeDescriptionRepository.save(existing);
                    },
                    () -> placeDescriptionRepository.save(PlaceDescription.builder()
                            .placeId(placeId)
                            .description(text)
                            .reviewCount(usableCount)
                            .build())
            );
        } catch (DataIntegrityViolationException e) {
            // 같은 장소를 동시에 생성해서 다른 쪽이 먼저 저장한 경우. 이미 저장돼 있으니 무시한다.
            log.debug("[description-ai] 이미 저장된 설명이 있어 건너뜁니다. placeId={}", placeId);
        }
    }
}
