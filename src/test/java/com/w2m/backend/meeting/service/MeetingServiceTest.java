package com.w2m.backend.meeting.service;

import com.w2m.backend.meeting.dto.request.ConfirmMeetingTimeRequest;
import com.w2m.backend.meeting.entity.Meeting;
import com.w2m.backend.meeting.event.MeetingTimeConfirmedEvent;
import com.w2m.backend.meeting.repository.MeetingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeetingServiceTest {

    @Mock private MeetingRepository meetingRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private MeetingService meetingService;

    private static final Long MEETING_ID = 1L;
    private static final Long HOST_ID = 10L;

    private static final LocalDateTime OLD_START = LocalDateTime.of(2026, 11, 2, 10, 0);
    private static final LocalDateTime OLD_END = LocalDateTime.of(2026, 11, 2, 13, 0);
    private static final LocalDateTime NEW_START = LocalDateTime.of(2026, 11, 3, 14, 0);
    private static final LocalDateTime NEW_END = LocalDateTime.of(2026, 11, 3, 16, 0);

    private Meeting meetingWithStatus(Meeting.MeetingStatus status) {
        Meeting meeting = new Meeting(HOST_ID, "테스트 모임", "설명", "INVITE1", Meeting.PlaceCategory.CAFE);
        meeting.confirmTime(OLD_START, OLD_END);
        meeting.setStatus(status);
        return meeting;
    }

    private ConfirmMeetingTimeRequest request() {
        ConfirmMeetingTimeRequest request = new ConfirmMeetingTimeRequest();
        ReflectionTestUtils.setField(request, "startDateTime", NEW_START);
        ReflectionTestUtils.setField(request, "endDateTime", NEW_END);
        return request;
    }

    @Test
    void 방장이_시간을_확정하면_시간이_저장되고_알림_이벤트가_발행된다() {
        Meeting meeting = meetingWithStatus(Meeting.MeetingStatus.COLLECTING_TIME);
        when(meetingRepository.findById(MEETING_ID)).thenReturn(Optional.of(meeting));

        meetingService.confirmMeetingTime(MEETING_ID, request(), HOST_ID);

        assertThat(meeting.getConfirmedStartDateTime()).isEqualTo(NEW_START);
        assertThat(meeting.getConfirmedEndDateTime()).isEqualTo(NEW_END);
        assertThat(meeting.getStatus()).isEqualTo(Meeting.MeetingStatus.COLLECTING_LOCATION);
        verify(eventPublisher).publishEvent(any(MeetingTimeConfirmedEvent.class));
    }

    @Test
    void 위치_입력이나_투표_중에는_시간을_다시_확정할_수_있다() {
        Meeting meeting = meetingWithStatus(Meeting.MeetingStatus.VOTING);
        when(meetingRepository.findById(MEETING_ID)).thenReturn(Optional.of(meeting));

        meetingService.confirmMeetingTime(MEETING_ID, request(), HOST_ID);

        assertThat(meeting.getConfirmedStartDateTime()).isEqualTo(NEW_START);
        assertThat(meeting.getStatus()).isEqualTo(Meeting.MeetingStatus.COLLECTING_LOCATION);
        verify(eventPublisher).publishEvent(any(MeetingTimeConfirmedEvent.class));
    }

    @Test
    void 최종_확정된_모임은_시간을_다시_확정할_수_없고_상태와_시간이_그대로다() {
        Meeting meeting = meetingWithStatus(Meeting.MeetingStatus.CONFIRMED);
        when(meetingRepository.findById(MEETING_ID)).thenReturn(Optional.of(meeting));

        assertThatThrownBy(() -> meetingService.confirmMeetingTime(MEETING_ID, request(), HOST_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 최종 확정된 모임입니다.");

        assertThat(meeting.getStatus()).isEqualTo(Meeting.MeetingStatus.CONFIRMED);
        assertThat(meeting.getConfirmedStartDateTime()).isEqualTo(OLD_START);
        assertThat(meeting.getConfirmedEndDateTime()).isEqualTo(OLD_END);
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void 방장이_아니면_시간을_확정할_수_없다() {
        Meeting meeting = meetingWithStatus(Meeting.MeetingStatus.COLLECTING_TIME);
        when(meetingRepository.findById(MEETING_ID)).thenReturn(Optional.of(meeting));

        assertThatThrownBy(() -> meetingService.confirmMeetingTime(MEETING_ID, request(), 999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("방장만 약속 시간을 확정할 수 있습니다.");

        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void 존재하지_않는_모임이면_예외() {
        when(meetingRepository.findById(MEETING_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> meetingService.confirmMeetingTime(MEETING_ID, request(), HOST_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("모임이 존재하지 않습니다.");
    }
}
