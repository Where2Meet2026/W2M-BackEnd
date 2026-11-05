package com.w2m.backend.location.service;

import com.w2m.backend.auth.entity.Provider;
import com.w2m.backend.auth.entity.User;
import com.w2m.backend.location.dto.request.SaveLocationRequest;
import com.w2m.backend.location.repository.ParticipantLocationRepository;
import com.w2m.backend.meeting.entity.Meeting;
import com.w2m.backend.meeting.event.MeetingLocationsCompletedEvent;
import com.w2m.backend.meeting.repository.MeetingRepository;
import com.w2m.backend.participant.entity.Participant;
import com.w2m.backend.participant.repository.ParticipantRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 마지막 참여자가 위치를 입력한 순간에만 투표 안내 이벤트가 나가는지 확인. (DB 없이 mock)
@ExtendWith(MockitoExtension.class)
class LocationServiceTest {

    @Mock private ParticipantLocationRepository participantLocationRepository;
    @Mock private ParticipantRepository participantRepository;
    @Mock private MeetingRepository meetingRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private LocationService locationService;

    private static final Long MEETING_ID = 1L;
    private static final Long PARTICIPANT_ID = 5L;
    private static final Long USER_ID = 10L;

    private final Meeting meeting = meeting();

    private static Meeting meeting() {
        Meeting meeting = new Meeting(USER_ID, "마포구 주민들", "설명", "CODE1", Meeting.PlaceCategory.CAFE);
        ReflectionTestUtils.setField(meeting, "id", MEETING_ID);
        return meeting;
    }

    private Participant participant(Long userId, boolean locationSelected) {
        User user = User.builder().email("u" + userId + "@a.com").name("user" + userId).provider(Provider.LOCAL).build();
        ReflectionTestUtils.setField(user, "id", userId);
        Participant participant = new Participant(meeting, user, Participant.ParticipantRole.PARTICIPANT);
        participant.updateLocationSelected(locationSelected);
        return participant;
    }

    private SaveLocationRequest request() {
        SaveLocationRequest request = new SaveLocationRequest();
        ReflectionTestUtils.setField(request, "address", "서울 마포구");
        ReflectionTestUtils.setField(request, "latitude", 37.55);
        ReflectionTestUtils.setField(request, "longitude", 126.92);
        return request;
    }

    private void givenSaving(Participant me) {
        when(participantRepository.findById(PARTICIPANT_ID)).thenReturn(Optional.of(me));
        when(meetingRepository.findWithLockById(MEETING_ID)).thenReturn(Optional.of(meeting));
        when(participantLocationRepository.findByParticipant(me)).thenReturn(Optional.empty());
    }

    @Test
    void 마지막_참여자가_위치를_입력하면_투표_안내_이벤트를_발행한다() {
        Participant me = participant(USER_ID, false);
        Participant other = participant(20L, true);
        givenSaving(me);
        when(participantRepository.findLatestByMeetingId(MEETING_ID)).thenReturn(List.of(me, other));

        locationService.saveLocation(PARTICIPANT_ID, request(), USER_ID);

        assertThat(me.isLocationSelected()).isTrue();
        verify(eventPublisher).publishEvent(new MeetingLocationsCompletedEvent(MEETING_ID));
    }

    @Test
    void 아직_위치를_안_넣은_참여자가_있으면_이벤트를_발행하지_않는다() {
        Participant me = participant(USER_ID, false);
        Participant other = participant(20L, false);
        givenSaving(me);
        when(participantRepository.findLatestByMeetingId(MEETING_ID)).thenReturn(List.of(me, other));

        locationService.saveLocation(PARTICIPANT_ID, request(), USER_ID);

        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void 이미_입력한_사람이_위치만_바꾸면_다시_보내지_않는다() {
        Participant me = participant(USER_ID, true);
        givenSaving(me);

        locationService.saveLocation(PARTICIPANT_ID, request(), USER_ID);

        verify(participantRepository, never()).findLatestByMeetingId(anyLong());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void 남의_위치는_저장하지_못하고_모임도_잠그지_않는다() {
        Participant someoneElse = participant(20L, false);
        when(participantRepository.findById(PARTICIPANT_ID)).thenReturn(Optional.of(someoneElse));

        assertThatThrownBy(() -> locationService.saveLocation(PARTICIPANT_ID, request(), USER_ID))
                .isInstanceOf(IllegalArgumentException.class);

        verify(meetingRepository, never()).findWithLockById(anyLong());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }
}
