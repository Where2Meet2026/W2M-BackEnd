package com.w2m.backend.review.service;

import com.w2m.backend.auth.entity.User;
import com.w2m.backend.auth.repository.UserRepository;
import com.w2m.backend.candidate.entity.PlaceCandidate;
import com.w2m.backend.candidate.repository.PlaceCandidateRepository;
import com.w2m.backend.meeting.entity.Meeting;
import com.w2m.backend.meeting.repository.MeetingRepository;
import com.w2m.backend.participant.repository.ParticipantRepository;
import com.w2m.backend.review.dto.request.CreateReviewRequest;
import com.w2m.backend.review.dto.response.PlaceReviewsResponse;
import com.w2m.backend.review.dto.response.ReviewResponse;
import com.w2m.backend.review.entity.Review;
import com.w2m.backend.review.event.ReviewCreatedEvent;
import com.w2m.backend.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");

    private final ReviewRepository reviewRepository;
    private final MeetingRepository meetingRepository;
    private final ParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final PlaceCandidateRepository placeCandidateRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Long createReview(Long meetingId, CreateReviewRequest request, Long userId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("모임을 찾을 수 없어요."));

        if (!participantRepository.existsByMeetingIdAndUserId(meetingId, userId)) {
            throw new IllegalArgumentException("이 모임의 참여자가 아니에요.");
        }

        // 투표로 확정된 장소가 있어야만(= 모임이 CONFIRMED여야만) 리뷰를 쓸 수 있다.
        if (meeting.getStatus() != Meeting.MeetingStatus.CONFIRMED) {
            throw new IllegalStateException("아직 확정되지 않은 모임이에요.");
        }

        // 다녀온 뒤에만 리뷰를 쓸 수 있다. 약속 시간은 사용자가 한국 시간으로 고른 값이라 서버 시간대와 무관하게 한국 시간으로 비교한다.
        LocalDateTime endDateTime = meeting.getConfirmedEndDateTime();
        if (endDateTime == null || LocalDateTime.now(KOREA).isBefore(endDateTime)) {
            throw new IllegalStateException("약속이 끝난 뒤에 리뷰를 쓸 수 있어요.");
        }

        PlaceCandidate confirmedPlace = meeting.getConfirmedCandidate();

        if (reviewRepository.existsByPlaceIdAndUserId(confirmedPlace.getKakaoPlaceId(), userId)) {
            throw new IllegalStateException("이미 이 장소에 리뷰를 썼어요.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없어요."));

        Review review = Review.builder()
                .user(user)
                .placeId(confirmedPlace.getKakaoPlaceId())
                .placeName(confirmedPlace.getPlaceName())
                .placeAddress(confirmedPlace.getAddress())
                .meetingId(meetingId)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        Long reviewId;
        try {
            // 동시에 두 번 요청되면 위의 exists 검사를 둘 다 통과할 수 있어서, 유니크 제약 위반도 같은 오류로 처리한다.
            reviewId = reviewRepository.saveAndFlush(review).getId();
        } catch (DataIntegrityViolationException e) {
            if (isUniqueViolation(e)) {
                throw new IllegalStateException("이미 이 장소에 리뷰를 썼어요.");
            }
            // 중복이 아닌 무결성 오류(NOT NULL, 컬럼 길이, 옛 스키마 제약 등)를 "이미 썼어요"로 가리지 않도록 원인을 남긴다.
            log.error("[review] 리뷰 저장 실패 (중복 아님) meetingId={} userId={} placeId={}",
                    meetingId, userId, review.getPlaceId(), e);
            throw new IllegalStateException("리뷰를 저장하지 못했어요. 잠시 후 다시 시도해 주세요.", e);
        }

        // 커밋 후 백그라운드에서 이 장소의 설명 문구를 다시 만든다 (rag 패키지의 리스너가 처리).
        eventPublisher.publishEvent(new ReviewCreatedEvent(review.getPlaceId(), review.getPlaceName()));
        return reviewId;
    }

    // 원인 체인에 유니크 제약 위반이 있으면 진짜 중복이다.
    private static boolean isUniqueViolation(DataIntegrityViolationException e) {
        if (e instanceof DuplicateKeyException) {
            return true;
        }
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof ConstraintViolationException cve
                    && cve.getKind() == ConstraintViolationException.ConstraintKind.UNIQUE) {
                return true;
            }
        }
        return false;
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviews(Long meetingId, Long userId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("모임을 찾을 수 없어요."));

        if (!participantRepository.existsByMeetingIdAndUserId(meetingId, userId)) {
            throw new IllegalArgumentException("이 모임의 참여자가 아니에요.");
        }

        // 확정 장소가 없으면 쌓인 리뷰도 없다.
        PlaceCandidate confirmedPlace = meeting.getConfirmedCandidate();
        if (confirmedPlace == null) {
            return List.of();
        }

        // 이 모임이 아니라 장소 기준으로 조회해서, 다른 모임 사람들이 쓴 리뷰도 함께 보여준다. (작성자는 계속 익명)
        return reviewRepository.findByPlaceIdOrderByCreatedAtDesc(confirmedPlace.getKakaoPlaceId()).stream()
                .map(ReviewResponse::from)
                .toList();
    }

    // 투표 중인 후보 장소에 누적된 리뷰 조회. 후보는 아직 확정 전이라 모임의 확정 장소가 아니라 후보 id로 장소를 찾는다.
    @Transactional(readOnly = true)
    public PlaceReviewsResponse getCandidateReviews(Long meetingId, Long candidateId, Long userId) {
        if (!meetingRepository.existsById(meetingId)) {
            throw new IllegalArgumentException("모임을 찾을 수 없어요.");
        }

        if (!participantRepository.existsByMeetingIdAndUserId(meetingId, userId)) {
            throw new IllegalArgumentException("이 모임의 참여자가 아니에요.");
        }

        // 다른 모임의 후보 id로 조회하지 못하도록 이 모임의 후보 중에서만 찾는다.
        PlaceCandidate candidate = placeCandidateRepository.findByMeetingId(meetingId).stream()
                .filter(c -> c.getId().equals(candidateId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("후보를 찾을 수 없어요."));

        List<ReviewResponse> reviews = reviewRepository.findByPlaceIdOrderByCreatedAtDesc(candidate.getKakaoPlaceId())
                .stream()
                .map(ReviewResponse::from)
                .toList();

        return PlaceReviewsResponse.builder()
                .placeName(candidate.getPlaceName())
                .placeAddress(candidate.getAddress())
                .reviews(reviews)
                .build();
    }
}
