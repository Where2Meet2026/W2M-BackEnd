package com.w2m.backend.vote.service;

import com.w2m.backend.candidate.entity.PlaceCandidate;
import com.w2m.backend.candidate.repository.CandidateReactionRepository;
import com.w2m.backend.candidate.repository.PlaceCandidateRepository;
import com.w2m.backend.meeting.entity.Meeting;
import com.w2m.backend.meeting.event.MeetingPlaceConfirmedEvent;
import com.w2m.backend.meeting.repository.MeetingRepository;
import com.w2m.backend.participant.entity.Participant;
import com.w2m.backend.participant.repository.ParticipantRepository;
import com.w2m.backend.vote.repository.VoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 마지막 투표로 장소가 확정될 때만 확정 알림 이벤트가 나가는지 확인. (DB 없이 mock, 통합 테스트는 VoteServiceTest)
@ExtendWith(MockitoExtension.class)
class VoteServiceEventTest {

    @Mock private MeetingRepository meetingRepository;
    @Mock private ParticipantRepository participantRepository;
    @Mock private PlaceCandidateRepository placeCandidateRepository;
    @Mock private VoteRepository voteRepository;
    @Mock private CandidateReactionRepository candidateReactionRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private VoteService voteService;

    private static final Long MEETING_ID = 1L;
    private static final Long CANDIDATE_ID = 7L;
    private static final Long USER_ID = 10L;
    private static final Long PARTICIPANT_ID = 5L;

    private Participant me;
    private PlaceCandidate candidate;

    @BeforeEach
    void setUp() {
        Meeting meeting = new Meeting(USER_ID, "마포구 주민들", "설명", "CODE1", Meeting.PlaceCategory.CAFE);
        ReflectionTestUtils.setField(meeting, "id", MEETING_ID);
        me = new Participant(meeting, null, Participant.ParticipantRole.HOST);
        ReflectionTestUtils.setField(me, "id", PARTICIPANT_ID);
        candidate = PlaceCandidate.builder()
                .meeting(meeting).kakaoPlaceId("kakao-1").placeName("돝고기506")
                .address("서울").latitude(37.5).longitude(127.0)
                .type(PlaceCandidate.CandidateType.FASTEST)
                .avgDistanceMeters(100.0).maxDistanceMeters(100.0).build();

        when(meetingRepository.findWithLockById(MEETING_ID)).thenReturn(Optional.of(meeting));
        when(participantRepository.findByMeetingIdAndUserId(MEETING_ID, USER_ID)).thenReturn(Optional.of(me));
        when(placeCandidateRepository.findById(CANDIDATE_ID)).thenReturn(Optional.of(candidate));
        when(voteRepository.findByMeetingIdAndParticipantId(MEETING_ID, PARTICIPANT_ID)).thenReturn(Optional.empty());
    }

    @Test
    void 마지막_투표로_장소가_확정되면_확정_알림_이벤트를_발행한다() {
        when(participantRepository.findByMeetingId(MEETING_ID)).thenReturn(List.of(me));
        when(voteRepository.countByMeetingId(MEETING_ID)).thenReturn(1L);
        when(placeCandidateRepository.findByMeetingId(MEETING_ID)).thenReturn(List.of(candidate));

        assertThat(voteService.castVote(MEETING_ID, CANDIDATE_ID, USER_ID)).isTrue();

        verify(eventPublisher).publishEvent(new MeetingPlaceConfirmedEvent(MEETING_ID));
    }

    @Test
    void 아직_투표하지_않은_사람이_있으면_이벤트를_발행하지_않는다() {
        when(participantRepository.findByMeetingId(MEETING_ID)).thenReturn(List.of(me, new Participant(null, null, Participant.ParticipantRole.PARTICIPANT)));
        when(voteRepository.countByMeetingId(MEETING_ID)).thenReturn(1L);

        assertThat(voteService.castVote(MEETING_ID, CANDIDATE_ID, USER_ID)).isFalse();

        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }
}
