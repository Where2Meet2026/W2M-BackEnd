package com.w2m.backend.candidate.service;

import com.w2m.backend.candidate.client.KakaoLocalApiClient;
import com.w2m.backend.candidate.client.ODsayClient;
import com.w2m.backend.candidate.dto.response.KakaoLocalSearchResponse;
import com.w2m.backend.candidate.dto.response.KakaoLocalSearchResponse.KakaoPlaceDto;
import com.w2m.backend.candidate.entity.CandidateReaction;
import com.w2m.backend.candidate.entity.PlaceCandidate;
import com.w2m.backend.candidate.entity.RejectedPlace;
import com.w2m.backend.candidate.repository.CandidateReactionRepository;
import com.w2m.backend.candidate.repository.PlaceCandidateRepository;
import com.w2m.backend.candidate.repository.RejectedPlaceRepository;
import com.w2m.backend.location.entity.ParticipantLocation;
import com.w2m.backend.location.repository.ParticipantLocationRepository;
import com.w2m.backend.meeting.entity.Meeting;
import com.w2m.backend.meeting.repository.MeetingRepository;
import com.w2m.backend.participant.entity.Participant;
import com.w2m.backend.participant.repository.ParticipantRepository;
import com.w2m.backend.vote.repository.VoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 싫어요 과반으로 교체할 때, 이전에 빠진 장소를 다시 고르지 않는지 확인한다. (DB/카카오 없이 mock)
@ExtendWith(MockitoExtension.class)
class PlaceCandidateServiceReplaceTest {

    @Mock private ParticipantLocationRepository participantLocationRepository;
    @Mock private PlaceCandidateRepository placeCandidateRepository;
    @Mock private MeetingRepository meetingRepository;
    @Mock private KakaoLocalApiClient kakaoLocalApiClient;
    @Mock private CandidateReactionRepository candidateReactionRepository;
    @Mock private VoteRepository voteRepository;
    @Mock private RejectedPlaceRepository rejectedPlaceRepository;
    @Mock private ODsayClient odsayClient;
    @Mock private ParticipantRepository participantRepository;

    @InjectMocks
    private PlaceCandidateService service;

    private static final Long MEETING_ID = 1L;
    private static final Long CANDIDATE_ID = 5L;
    private static final Long USER_ID = 10L;

    private Meeting meeting;
    private PlaceCandidate current;

    @BeforeEach
    void setUp() {
        meeting = new Meeting(USER_ID, "마포구 주민들", "설명", "CODE1", Meeting.PlaceCategory.CAFE);
        ReflectionTestUtils.setField(meeting, "id", MEETING_ID);
        current = PlaceCandidate.builder()
                .meeting(meeting).kakaoPlaceId("current").placeName("지금 후보")
                .address("서울").latitude(37.5).longitude(127.0)
                .type(PlaceCandidate.CandidateType.FASTEST)
                .avgDistanceMeters(100.0).maxDistanceMeters(100.0).build();
        ReflectionTestUtils.setField(current, "id", CANDIDATE_ID);

        Participant me = mock(Participant.class);
        ParticipantLocation location = mock(ParticipantLocation.class);
        when(location.getLatitude()).thenReturn(37.5);
        when(location.getLongitude()).thenReturn(127.0);
        when(participantRepository.findByMeetingIdAndUserId(MEETING_ID, USER_ID)).thenReturn(Optional.of(me));
        when(participantRepository.findByMeetingId(MEETING_ID)).thenReturn(List.of(me));
        when(placeCandidateRepository.findById(CANDIDATE_ID)).thenReturn(Optional.of(current));
        when(placeCandidateRepository.findByMeetingId(MEETING_ID)).thenReturn(List.of(current));
        when(participantLocationRepository.findAllByMeetingId(MEETING_ID)).thenReturn(List.of(location));
        when(candidateReactionRepository.findByCandidateAndParticipant(current, me)).thenReturn(Optional.empty());
        when(candidateReactionRepository.countByCandidateAndReactionType(current, CandidateReaction.ReactionType.DISLIKE))
                .thenReturn(1L);
    }

    private KakaoPlaceDto place(String id, String longitude) {
        return new KakaoPlaceDto(id, "장소 " + id, "서울", "카페", "CE7", longitude, "37.5");
    }

    private void givenSearchResult(KakaoPlaceDto... places) {
        when(kakaoLocalApiClient.searchByCategory(anyDouble(), anyDouble(), anyInt(), anyString()))
                .thenReturn(new KakaoLocalSearchResponse(List.of(places)));
    }

    private RejectedPlace rejected(String placeId) {
        return RejectedPlace.builder().meeting(meeting).kakaoPlaceId(placeId).build();
    }

    @Test
    void 이전에_싫어요로_빠진_장소는_더_가까워도_다시_고르지_않는다() {
        when(rejectedPlaceRepository.findByMeetingId(MEETING_ID)).thenReturn(List.of(rejected("old")));
        givenSearchResult(place("old", "127.001"), place("next", "127.002"), place("far", "127.003"));

        service.toggleReaction(MEETING_ID, CANDIDATE_ID, USER_ID, CandidateReaction.ReactionType.DISLIKE);

        ArgumentCaptor<PlaceCandidate> saved = ArgumentCaptor.forClass(PlaceCandidate.class);
        verify(placeCandidateRepository).save(saved.capture());
        assertThat(saved.getValue().getKakaoPlaceId()).isEqualTo("next");
        assertThat(saved.getValue().getType()).isEqualTo(PlaceCandidate.CandidateType.FASTEST);
    }

    @Test
    void 교체되는_장소를_다음_교체에서_제외하도록_기록한다() {
        when(rejectedPlaceRepository.findByMeetingId(MEETING_ID)).thenReturn(List.of());
        givenSearchResult(place("next", "127.002"));

        service.toggleReaction(MEETING_ID, CANDIDATE_ID, USER_ID, CandidateReaction.ReactionType.DISLIKE);

        ArgumentCaptor<RejectedPlace> saved = ArgumentCaptor.forClass(RejectedPlace.class);
        verify(rejectedPlaceRepository).save(saved.capture());
        assertThat(saved.getValue().getKakaoPlaceId()).isEqualTo("current");
        assertThat(saved.getValue().getMeeting()).isEqualTo(meeting);
    }

    @Test
    void 남은_장소가_모두_제외되면_후보를_지우지_않고_안내한다() {
        when(rejectedPlaceRepository.findByMeetingId(MEETING_ID)).thenReturn(List.of(rejected("old")));
        givenSearchResult(place("old", "127.001"));

        assertThatThrownBy(() ->
                service.toggleReaction(MEETING_ID, CANDIDATE_ID, USER_ID, CandidateReaction.ReactionType.DISLIKE))
                .isInstanceOf(IllegalStateException.class);

        verify(placeCandidateRepository, never()).delete(any());
        verify(rejectedPlaceRepository, never()).save(any());
    }
}
