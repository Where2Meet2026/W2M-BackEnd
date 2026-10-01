package com.w2m.backend.review.service;

import com.w2m.backend.auth.entity.User;
import com.w2m.backend.auth.repository.UserRepository;
import com.w2m.backend.candidate.entity.PlaceCandidate;
import com.w2m.backend.meeting.entity.Meeting;
import com.w2m.backend.meeting.repository.MeetingRepository;
import com.w2m.backend.participant.repository.ParticipantRepository;
import com.w2m.backend.review.dto.request.CreateReviewRequest;
import com.w2m.backend.review.dto.response.ReviewResponse;
import com.w2m.backend.review.entity.Review;
import com.w2m.backend.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
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

        return reviewRepository.save(review).getId();
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviews(Long meetingId, Long userId) {
        if (!meetingRepository.existsById(meetingId)) {
            throw new IllegalArgumentException("존재하지 않는 모임입니다.");
        }

        if (!participantRepository.existsByMeetingIdAndUserId(meetingId, userId)) {
            throw new IllegalArgumentException("이 모임의 참여자가 아닙니다.");
        }

        return reviewRepository.findByMeetingIdOrderByCreatedAtDesc(meetingId).stream()
                .map(ReviewResponse::from)
                .toList();
    }
}
