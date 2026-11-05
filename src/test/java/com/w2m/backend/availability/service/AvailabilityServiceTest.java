package com.w2m.backend.availability.service;

import com.w2m.backend.availability.dto.request.SaveAvailabilityRequest;
import com.w2m.backend.availability.repository.AvailabilityRepository;
import com.w2m.backend.meeting.entity.Meeting;
import com.w2m.backend.meeting.event.MeetingTimeInputCompletedEvent;
import com.w2m.backend.meeting.repository.MeetingRepository;
import com.w2m.backend.participant.entity.Participant;
import com.w2m.backend.participant.repository.ParticipantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 방장을 뺀 마지막 참여자가 시간을 입력한 순간에만 확정 요청 이벤트가 나가는지 확인. (DB 없이 mock)
@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

    @Mock private AvailabilityRepository availabilityRepository;
    @Mock private ParticipantRepository participantRepository;
    @Mock private MeetingRepository meetingRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AvailabilityService availabilityService;

    private static final Long MEETING_ID = 1L;
    private static final Long PARTICIPANT_ID = 5L;

    private Meeting meeting;
    private SaveAvailabilityRequest request;

    @BeforeEach
    void setUp() {
        meeting = new Meeting(10L, "마포구 주민들", "설명", "CODE1", Meeting.PlaceCategory.CAFE);
        ReflectionTestUtils.setField(meeting, "id", MEETING_ID);
        request = new SaveAvailabilityRequest();
        ReflectionTestUtils.setField(request, "timeRanges", List.of());
        lenient().when(meetingRepository.findWithLockById(MEETING_ID)).thenReturn(Optional.of(meeting));
    }

    private Participant participant(Participant.ParticipantRole role, boolean timeSelected) {
        Participant participant = new Participant(meeting, null, role);
        participant.updateTimeSelected(timeSelected);
        return participant;
    }

    private void givenSaving(Participant me) {
        when(participantRepository.findById(PARTICIPANT_ID)).thenReturn(Optional.of(me));
    }

    @Test
    void 방장이_입력_전이어도_나머지_전원이_입력하면_이벤트를_발행한다() {
        Participant host = participant(Participant.ParticipantRole.HOST, false);
        Participant other = participant(Participant.ParticipantRole.PARTICIPANT, true);
        Participant me = participant(Participant.ParticipantRole.PARTICIPANT, false);
        givenSaving(me);
        when(participantRepository.findLatestByMeetingId(MEETING_ID)).thenReturn(List.of(host, other, me));

        availabilityService.saveAvailabilities(PARTICIPANT_ID, request);

        assertThat(me.isTimeSelected()).isTrue();
        verify(eventPublisher).publishEvent(new MeetingTimeInputCompletedEvent(MEETING_ID));
    }

    @Test
    void 아직_입력하지_않은_참여자가_있으면_발행하지_않는다() {
        Participant other = participant(Participant.ParticipantRole.PARTICIPANT, false);
        Participant me = participant(Participant.ParticipantRole.PARTICIPANT, false);
        givenSaving(me);
        when(participantRepository.findLatestByMeetingId(MEETING_ID)).thenReturn(List.of(other, me));

        availabilityService.saveAvailabilities(PARTICIPANT_ID, request);

        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void 방장의_입력으로는_발행하지_않는다() {
        givenSaving(participant(Participant.ParticipantRole.HOST, false));

        availabilityService.saveAvailabilities(PARTICIPANT_ID, request);

        verify(participantRepository, never()).findLatestByMeetingId(anyLong());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void 이미_입력한_참여자가_시간을_수정하면_다시_보내지_않는다() {
        givenSaving(participant(Participant.ParticipantRole.PARTICIPANT, true));

        availabilityService.saveAvailabilities(PARTICIPANT_ID, request);

        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void 시간이_이미_확정됐으면_보내지_않는다() {
        LocalDateTime start = LocalDateTime.of(2026, 11, 20, 18, 0);
        meeting.confirmTime(start, start.plusHours(2));
        givenSaving(participant(Participant.ParticipantRole.PARTICIPANT, false));

        availabilityService.saveAvailabilities(PARTICIPANT_ID, request);

        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }
}
