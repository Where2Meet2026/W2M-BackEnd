package com.w2m.backend.review.service;

import com.w2m.backend.auth.entity.Provider;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

// 서버(및 DB)를 띄우지 않고 레포지토리를 전부 mock으로 대체하는 순수 단위 테스트.
@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private MeetingRepository meetingRepository;
    @Mock private ParticipantRepository participantRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private ReviewService reviewService;

    private static final Long MEETING_ID = 1L;
    private static final Long USER_ID = 10L;

    private PlaceCandidate confirmedCandidate() {
        return PlaceCandidate.builder()
                .kakaoPlaceId("kakao-1")
                .placeName("감성 루프탑 카페")
                .address("서울 강남구")
                .latitude(37.5)
                .longitude(127.0)
                .type(PlaceCandidate.CandidateType.OPTIMAL)
                .avgDistanceMeters(300.0)
                .maxDistanceMeters(500.0)
                .build();
    }

    private Meeting meetingWithStatus(Meeting.MeetingStatus status, PlaceCandidate confirmedCandidate) {
        Meeting meeting = new Meeting(99L, "테스트 모임", "설명", "INVITE1", Meeting.PlaceCategory.CAFE);
        meeting.setStatus(status);
        meeting.setConfirmedCandidate(confirmedCandidate);
        return meeting;
    }

    private User user() {
        return User.builder()
                .email("test@w2m.com")
                .name("석영")
                .provider(Provider.LOCAL)
                .build();
    }

    @Test
    void 확정된_모임이면_리뷰를_생성한다() {
        PlaceCandidate candidate = confirmedCandidate();
        Meeting meeting = meetingWithStatus(Meeting.MeetingStatus.CONFIRMED, candidate);
        User user = user();
        CreateReviewRequest request = new CreateReviewRequest(5, "조용하고 좋았어요");

        when(meetingRepository.findById(MEETING_ID)).thenReturn(Optional.of(meeting));
        when(participantRepository.existsByMeetingIdAndUserId(MEETING_ID, USER_ID)).thenReturn(true);
        when(reviewRepository.existsByPlaceIdAndUserId("kakao-1", USER_ID)).thenReturn(false);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 100L);
            return saved;
        });

        Long reviewId = reviewService.createReview(MEETING_ID, request, USER_ID);

        assertThat(reviewId).isEqualTo(100L);

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(captor.capture());
        Review saved = captor.getValue();
        assertThat(saved.getPlaceId()).isEqualTo("kakao-1");
        assertThat(saved.getPlaceName()).isEqualTo("감성 루프탑 카페");
        assertThat(saved.getPlaceAddress()).isEqualTo("서울 강남구");
        assertThat(saved.getMeetingId()).isEqualTo(MEETING_ID);
        assertThat(saved.getRating()).isEqualTo(5);
        assertThat(saved.getComment()).isEqualTo("조용하고 좋았어요");
        assertThat(saved.getUser()).isEqualTo(user);
    }

    @Test
    void 존재하지_않는_모임이면_예외() {
        when(meetingRepository.findById(MEETING_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                reviewService.createReview(MEETING_ID, new CreateReviewRequest(5, "좋아요"), USER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 모임입니다.");

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void 이_모임_참여자가_아니면_예외() {
        Meeting meeting = meetingWithStatus(Meeting.MeetingStatus.CONFIRMED, confirmedCandidate());
        when(meetingRepository.findById(MEETING_ID)).thenReturn(Optional.of(meeting));
        when(participantRepository.existsByMeetingIdAndUserId(MEETING_ID, USER_ID)).thenReturn(false);

        assertThatThrownBy(() ->
                reviewService.createReview(MEETING_ID, new CreateReviewRequest(5, "좋아요"), USER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이 모임의 참여자가 아닙니다.");

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void 아직_확정되지_않은_모임이면_예외() {
        Meeting meeting = meetingWithStatus(Meeting.MeetingStatus.VOTING, null);
        when(meetingRepository.findById(MEETING_ID)).thenReturn(Optional.of(meeting));
        when(participantRepository.existsByMeetingIdAndUserId(MEETING_ID, USER_ID)).thenReturn(true);

        assertThatThrownBy(() ->
                reviewService.createReview(MEETING_ID, new CreateReviewRequest(5, "좋아요"), USER_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("아직 확정되지 않은 모임입니다.");

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void 이미_이_장소에_리뷰를_작성했다면_예외() {
        PlaceCandidate candidate = confirmedCandidate();
        Meeting meeting = meetingWithStatus(Meeting.MeetingStatus.CONFIRMED, candidate);
        when(meetingRepository.findById(MEETING_ID)).thenReturn(Optional.of(meeting));
        when(participantRepository.existsByMeetingIdAndUserId(MEETING_ID, USER_ID)).thenReturn(true);
        when(reviewRepository.existsByPlaceIdAndUserId("kakao-1", USER_ID)).thenReturn(true);

        assertThatThrownBy(() ->
                reviewService.createReview(MEETING_ID, new CreateReviewRequest(5, "좋아요"), USER_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 이 장소에 리뷰를 작성했습니다.");

        verify(reviewRepository, never()).save(any());
        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    void 리뷰_목록을_최신순으로_조회한다() {
        when(meetingRepository.existsById(MEETING_ID)).thenReturn(true);
        when(participantRepository.existsByMeetingIdAndUserId(MEETING_ID, USER_ID)).thenReturn(true);

        Review review1 = buildPersistedReview(5, "최고였어요", LocalDateTime.of(2026, 9, 10, 10, 0));
        Review review2 = buildPersistedReview(3, "그냥 그랬어요", LocalDateTime.of(2026, 9, 5, 10, 0));
        when(reviewRepository.findByMeetingIdOrderByCreatedAtDesc(MEETING_ID))
                .thenReturn(List.of(review1, review2));

        List<ReviewResponse> responses = reviewService.getReviews(MEETING_ID, USER_ID);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getRating()).isEqualTo(5);
        assertThat(responses.get(0).getComment()).isEqualTo("최고였어요");
        assertThat(responses.get(1).getRating()).isEqualTo(3);
    }

    @Test
    void 목록_조회시_존재하지_않는_모임이면_예외() {
        when(meetingRepository.existsById(MEETING_ID)).thenReturn(false);

        assertThatThrownBy(() -> reviewService.getReviews(MEETING_ID, USER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 모임입니다.");

        verify(reviewRepository, never()).findByMeetingIdOrderByCreatedAtDesc(any());
    }

    @Test
    void 목록_조회시_참여자가_아니면_예외() {
        when(meetingRepository.existsById(MEETING_ID)).thenReturn(true);
        when(participantRepository.existsByMeetingIdAndUserId(MEETING_ID, USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> reviewService.getReviews(MEETING_ID, USER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이 모임의 참여자가 아닙니다.");

        verify(reviewRepository, never()).findByMeetingIdOrderByCreatedAtDesc(any());
    }

    private Review buildPersistedReview(int rating, String comment, LocalDateTime createdAt) {
        Review review = Review.builder()
                .user(user())
                .placeId("kakao-1")
                .placeName("감성 루프탑 카페")
                .placeAddress("서울 강남구")
                .meetingId(MEETING_ID)
                .rating(rating)
                .comment(comment)
                .build();
        // @PrePersist(onCreate)는 실제 JPA 저장 시에만 동작하므로, mock 응답용으로 직접 세팅.
        ReflectionTestUtils.setField(review, "createdAt", createdAt);
        return review;
    }
}
