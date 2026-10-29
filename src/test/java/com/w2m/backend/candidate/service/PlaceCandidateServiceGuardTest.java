package com.w2m.backend.candidate.service;

import com.w2m.backend.candidate.client.KakaoLocalApiClient;
import com.w2m.backend.candidate.dto.response.KakaoLocalSearchResponse;
import com.w2m.backend.candidate.entity.CandidateReaction;
import com.w2m.backend.candidate.entity.PlaceCandidate;
import com.w2m.backend.location.entity.ParticipantLocation;
import com.w2m.backend.candidate.repository.CandidateReactionRepository;
import com.w2m.backend.candidate.repository.PlaceCandidateRepository;
import com.w2m.backend.location.repository.ParticipantLocationRepository;
import com.w2m.backend.meeting.entity.Meeting;
import com.w2m.backend.meeting.repository.MeetingRepository;
import com.w2m.backend.participant.entity.Participant;
import com.w2m.backend.participant.repository.ParticipantRepository;
import com.w2m.backend.rag.service.PlaceDescriptionService;
import com.w2m.backend.vote.repository.VoteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 후보 최초 생성 가드: 전원이 위치를 입력하기 전에는 후보를 만들지 않는다. (DB/Kakao 없이 mock)
@ExtendWith(MockitoExtension.class)
class PlaceCandidateServiceGuardTest {

    @Mock private ParticipantLocationRepository participantLocationRepository;
    @Mock private PlaceCandidateRepository placeCandidateRepository;
    @Mock private MeetingRepository meetingRepository;
    @Mock private KakaoLocalApiClient kakaoLocalApiClient;
    @Mock private CandidateReactionRepository candidateReactionRepository;
    @Mock private VoteRepository voteRepository;
    @Mock private ParticipantRepository participantRepository;
    @Mock private PlaceDescriptionService placeDescriptionService;

    @InjectMocks
    private PlaceCandidateService service;

    private static final Long MEETING_ID = 1L;
    private static final Long USER_ID = 10L;

    private Participant participant(boolean locationSelected) {
        Participant participant = mock(Participant.class);
        when(participant.isLocationSelected()).thenReturn(locationSelected);
        return participant;
    }

    @Test
    void 위치를_입력하지_않은_참여자가_있으면_후보를_만들지_않고_409용_예외를_던진다() {
        when(participantRepository.findByMeetingIdAndUserId(MEETING_ID, USER_ID)).thenReturn(Optional.of(mock(Participant.class)));
        when(placeCandidateRepository.findByMeetingId(MEETING_ID)).thenReturn(List.of());
        when(meetingRepository.findWithLockById(MEETING_ID)).thenReturn(Optional.of(mock(Meeting.class)));
        List<Participant> participants = List.of(participant(true), participant(false));
        when(participantRepository.findByMeetingId(MEETING_ID)).thenReturn(participants);

        assertThatThrownBy(() -> service.getCandidates(MEETING_ID, USER_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("모든 참여자");

        verify(placeCandidateRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
        verify(kakaoLocalApiClient, never())
                .searchByCategory(org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyDouble(),
                        org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void 참여자가_없는_모임도_후보를_만들지_않는다() {
        when(participantRepository.findByMeetingIdAndUserId(MEETING_ID, USER_ID)).thenReturn(Optional.of(mock(Participant.class)));
        when(placeCandidateRepository.findByMeetingId(MEETING_ID)).thenReturn(List.of());
        when(meetingRepository.findWithLockById(MEETING_ID)).thenReturn(Optional.of(mock(Meeting.class)));
        when(participantRepository.findByMeetingId(MEETING_ID)).thenReturn(List.of());

        assertThatThrownBy(() -> service.getCandidates(MEETING_ID, USER_ID))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 모임_참여자가_아니면_후보_생성이나_잠금을_시도하지_않는다() {
        when(participantRepository.findByMeetingIdAndUserId(MEETING_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCandidates(MEETING_ID, USER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("참여자");

        verify(meetingRepository, never()).findWithLockById(MEETING_ID);
        verify(placeCandidateRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void 잠근_뒤_다시_확인했을_때_다른_요청이_이미_만들었으면_그_후보를_쓴다() {
        PlaceCandidate created = mock(PlaceCandidate.class);
        Participant me = mock(Participant.class);
        when(placeCandidateRepository.findByMeetingId(MEETING_ID)).thenReturn(List.of());             // 처음 조회: 비어 있음
        when(placeCandidateRepository.findLatestByMeetingId(MEETING_ID)).thenReturn(List.of(created)); // 잠근 뒤 재조회: 다른 요청이 방금 생성
        when(meetingRepository.findWithLockById(MEETING_ID)).thenReturn(Optional.of(mock(Meeting.class)));
        when(participantRepository.findByMeetingIdAndUserId(MEETING_ID, USER_ID)).thenReturn(Optional.of(me));
        when(candidateReactionRepository.countByCandidateAndReactionType(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(0L);
        when(candidateReactionRepository.findByCandidateAndParticipant(created, me)).thenReturn(Optional.empty());

        assertThat(service.getCandidates(MEETING_ID, USER_ID)).hasSize(1);

        verify(placeCandidateRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
        verify(participantRepository, never()).findByMeetingId(MEETING_ID);
    }

    @Test
    void 후보가_이미_있으면_잠금도_가드도_거치지_않는다() {
        PlaceCandidate existing = mock(PlaceCandidate.class);
        Participant me = mock(Participant.class);
        when(placeCandidateRepository.findByMeetingId(MEETING_ID)).thenReturn(List.of(existing));
        when(participantRepository.findByMeetingIdAndUserId(MEETING_ID, USER_ID)).thenReturn(Optional.of(me));
        when(candidateReactionRepository.findByCandidateAndParticipant(existing, me)).thenReturn(Optional.empty());

        assertThat(service.getCandidates(MEETING_ID, USER_ID)).hasSize(1);

        verify(meetingRepository, never()).findWithLockById(MEETING_ID);
    }

    @Test
    void 주변_장소가_3곳_미만이면_500이_아니라_안내_메시지의_예외를_던진다() {
        Participant me = mock(Participant.class);
        when(me.isLocationSelected()).thenReturn(true);
        Meeting meeting = mock(Meeting.class);
        ParticipantLocation location = mock(ParticipantLocation.class);
        when(location.getLatitude()).thenReturn(37.5);
        when(location.getLongitude()).thenReturn(127.0);
        when(participantRepository.findByMeetingIdAndUserId(MEETING_ID, USER_ID)).thenReturn(Optional.of(me));
        when(placeCandidateRepository.findByMeetingId(MEETING_ID)).thenReturn(List.of());
        when(meetingRepository.findWithLockById(MEETING_ID)).thenReturn(Optional.of(meeting));
        when(meetingRepository.findById(MEETING_ID)).thenReturn(Optional.of(meeting));
        when(participantRepository.findByMeetingId(MEETING_ID)).thenReturn(List.of(me));
        when(participantLocationRepository.findAllByMeetingId(MEETING_ID)).thenReturn(List.of(location));
        // 식당·카페 코드마다 한 곳씩, 모두 2곳
        when(kakaoLocalApiClient.searchByCategory(anyDouble(), anyDouble(), anyInt(), anyString()))
                .thenReturn(new KakaoLocalSearchResponse(List.of(
                        new KakaoLocalSearchResponse.KakaoPlaceDto("1", "가게1", "주소1", "음식점", "127.001", "37.501"))));

        assertThatThrownBy(() -> service.getCandidates(MEETING_ID, USER_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("3곳");
        verify(placeCandidateRepository, never()).saveAll(anyList());
    }

    @Test
    void 확정된_모임에서는_반응으로_후보를_바꾸지_못한다() {
        Participant me = mock(Participant.class);
        Meeting meeting = mock(Meeting.class);
        when(meeting.getStatus()).thenReturn(Meeting.MeetingStatus.CONFIRMED);
        PlaceCandidate candidate = mock(PlaceCandidate.class);
        when(candidate.getMeeting()).thenReturn(meeting);
        when(participantRepository.findByMeetingIdAndUserId(MEETING_ID, USER_ID)).thenReturn(Optional.of(me));
        when(placeCandidateRepository.findById(5L)).thenReturn(Optional.of(candidate));

        assertThatThrownBy(() -> service.toggleReaction(MEETING_ID, 5L, USER_ID, CandidateReaction.ReactionType.DISLIKE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("마감");
        verify(candidateReactionRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void 후보_조회_시_AI_설명이_저장된_장소만_그_설명으로_바꾸고_나머지는_기본_문구를_쓴다() {
        Participant me = mock(Participant.class);
        PlaceCandidate withAi = mock(PlaceCandidate.class);
        when(withAi.getKakaoPlaceId()).thenReturn("p-ai");
        when(withAi.getDescription()).thenReturn("평균 265m · 가장 빠른 동선");
        PlaceCandidate withoutAi = mock(PlaceCandidate.class);
        when(withoutAi.getKakaoPlaceId()).thenReturn("p-none");
        when(withoutAi.getDescription()).thenReturn("평균 310m · 종합 추천");
        when(participantRepository.findByMeetingIdAndUserId(MEETING_ID, USER_ID)).thenReturn(Optional.of(me));
        when(placeCandidateRepository.findByMeetingId(MEETING_ID)).thenReturn(List.of(withAi, withoutAi));
        when(placeDescriptionService.findCached(List.of("p-ai", "p-none")))
                .thenReturn(Map.of("p-ai", "차분한 분위기라는 평이 많아요."));

        var responses = service.getCandidates(MEETING_ID, USER_ID);

        assertThat(responses).extracting("description")
                .containsExactly("차분한 분위기라는 평이 많아요.", "평균 310m · 종합 추천");
        verify(placeDescriptionService, never()).resolve(anyString(), anyString()); // 조회 중에는 LLM을 부르지 않는다
    }
}
