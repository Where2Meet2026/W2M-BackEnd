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
import com.w2m.backend.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final MeetingRepository meetingRepository;
    private final ParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final PlaceCandidateRepository placeCandidateRepository;

    @Transactional
    public Long createReview(Long meetingId, CreateReviewRequest request, Long userId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 모임입니다."));

        if (!participantRepository.existsByMeetingIdAndUserId(meetingId, userId)) {
            throw new IllegalArgumentException("이 모임의 참여자가 아닙니다.");
        }

        // 투표로 확정된 장소가 있어야만(= 모임이 CONFIRMED여야만) 리뷰를 쓸 수 있다.
        if (meeting.getStatus() != Meeting.MeetingStatus.CONFIRMED) {
            throw new IllegalStateException("아직 확정되지 않은 모임입니다.");
        }

        PlaceCandidate confirmedPlace = meeting.getConfirmedCandidate();

        if (reviewRepository.existsByPlaceIdAndUserId(confirmedPlace.getKakaoPlaceId(), userId)) {
            throw new IllegalStateException("이미 이 장소에 리뷰를 작성했습니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Review review = Review.builder()
                .user(user)
                .placeId(confirmedPlace.getKakaoPlaceId())
                .placeName(confirmedPlace.getPlaceName())
                .placeAddress(confirmedPlace.getAddress())
                .meetingId(meetingId)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        try {
            // 동시에 두 번 요청되면 위의 exists 검사를 둘 다 통과할 수 있어서, 유니크 제약 위반도 같은 오류로 처리한다.
            return reviewRepository.saveAndFlush(review).getId();
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("이미 이 장소에 리뷰를 작성했습니다.");
        }
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviews(Long meetingId, Long userId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 모임입니다."));

        if (!participantRepository.existsByMeetingIdAndUserId(meetingId, userId)) {
            throw new IllegalArgumentException("이 모임의 참여자가 아닙니다.");
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
            throw new IllegalArgumentException("존재하지 않는 모임입니다.");
        }

        if (!participantRepository.existsByMeetingIdAndUserId(meetingId, userId)) {
            throw new IllegalArgumentException("이 모임의 참여자가 아닙니다.");
        }

        // 다른 모임의 후보 id로 조회하지 못하도록 이 모임의 후보 중에서만 찾는다.
        PlaceCandidate candidate = placeCandidateRepository.findByMeetingId(meetingId).stream()
                .filter(c -> c.getId().equals(candidateId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 후보입니다."));

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
