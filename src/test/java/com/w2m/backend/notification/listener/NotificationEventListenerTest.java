package com.w2m.backend.notification.listener;

import com.w2m.backend.meeting.event.MeetingLocationsCompletedEvent;
import com.w2m.backend.meeting.event.MeetingPlaceConfirmedEvent;
import com.w2m.backend.meeting.event.MeetingTimeConfirmedEvent;
import com.w2m.backend.meeting.event.MeetingTimeInputCompletedEvent;
import com.w2m.backend.notification.dispatch.NotificationDispatchService;
import com.w2m.backend.notification.push.PushTarget;
import com.w2m.backend.notification.push.WebPushSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 단계별 알림 문구와 이동 주소 확인. (실제 발송 없이 sender를 mock으로 대체)
@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock private NotificationDispatchService dispatchService;
    @Mock private WebPushSender webPushSender;

    @InjectMocks
    private NotificationEventListener listener;

    private static final Long MEETING_ID = 1L;
    private static final PushTarget TARGET = new PushTarget("endpoint-1", "p256dh", "auth");

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(listener, "webBaseUrl", "https://app.test");
    }

    private void givenOneSubscriberIn(String meetingTitle) {
        when(dispatchService.collectMeetingParticipantTargets(MEETING_ID)).thenReturn(List.of(TARGET));
        when(dispatchService.findMeetingTitle(MEETING_ID)).thenReturn(meetingTitle);
        when(webPushSender.send(any(), anyString(), anyString(), anyString())).thenReturn(WebPushSender.Result.SENT);
    }

    @Test
    void 참여자_시간_입력이_끝나면_방장에게만_시간_확정을_요청한다() {
        when(dispatchService.collectHostTargets(MEETING_ID)).thenReturn(List.of(TARGET));
        when(dispatchService.findMeetingTitle(MEETING_ID)).thenReturn("마포구 주민들");
        when(webPushSender.send(any(), anyString(), anyString(), anyString())).thenReturn(WebPushSender.Result.SENT);

        listener.onTimeInputCompleted(new MeetingTimeInputCompletedEvent(MEETING_ID));

        verify(webPushSender).send(TARGET,
                "[마포구 주민들] 모든 참가자들의 시간 입력이 완료되었습니다",
                "시간을 확정해주세요.",
                "https://app.test/recommendation/1");
        verify(dispatchService, never()).collectMeetingParticipantTargets(any());
    }

    @Test
    void 방장이_구독하지_않았으면_시간_확정_요청을_보내지_않는다() {
        when(dispatchService.collectHostTargets(MEETING_ID)).thenReturn(List.of());

        listener.onTimeInputCompleted(new MeetingTimeInputCompletedEvent(MEETING_ID));

        verify(webPushSender, never()).send(any(), anyString(), anyString(), anyString());
    }

    @Test
    void 시간이_확정되면_참가자_전원에게_출발_위치_입력을_요청한다() {
        givenOneSubscriberIn("마포구 주민들");

        listener.onMeetingTimeConfirmed(new MeetingTimeConfirmedEvent(MEETING_ID));

        verify(webPushSender).send(TARGET,
                "[마포구 주민들] 시간이 확정되었습니다",
                "각 참가자는 출발위치를 입력해주세요.",
                "https://app.test/location/1");
    }

    @Test
    void 모임_이름이_없으면_제목에_붙이지_않는다() {
        givenOneSubscriberIn(null);

        listener.onMeetingTimeConfirmed(new MeetingTimeConfirmedEvent(MEETING_ID));

        verify(webPushSender).send(TARGET, "시간이 확정되었습니다", "각 참가자는 출발위치를 입력해주세요.",
                "https://app.test/location/1");
    }

    @Test
    void 전원_위치_입력이_끝나면_후보_화면으로_투표를_안내한다() {
        givenOneSubscriberIn("마포구 주민들");

        listener.onLocationsCompleted(new MeetingLocationsCompletedEvent(MEETING_ID));

        verify(webPushSender).send(TARGET,
                "[마포구 주민들] 모두 출발 위치를 입력했어요",
                "추천 장소를 확인하고 투표해 주세요.",
                "https://app.test/candidates/1");
    }

    @Test
    void 장소가_확정되면_장소_이름과_함께_결과_화면으로_안내한다() {
        givenOneSubscriberIn("마포구 주민들");
        when(dispatchService.findConfirmedPlaceName(MEETING_ID)).thenReturn("돝고기506");

        listener.onPlaceConfirmed(new MeetingPlaceConfirmedEvent(MEETING_ID));

        verify(webPushSender).send(TARGET,
                "[마포구 주민들] 약속 장소가 확정되었습니다",
                "돝고기506에서 만나요.",
                "https://app.test/result/1");
    }

    @Test
    void 확정_장소_이름을_못_읽으면_기본_문구를_쓴다() {
        givenOneSubscriberIn(null);

        listener.onPlaceConfirmed(new MeetingPlaceConfirmedEvent(MEETING_ID));

        verify(webPushSender).send(TARGET, "약속 장소가 확정되었습니다", "확정된 약속을 확인해 주세요.",
                "https://app.test/result/1");
    }

    @Test
    void 구독자가_없으면_아무것도_보내지_않는다() {
        when(dispatchService.collectMeetingParticipantTargets(MEETING_ID)).thenReturn(List.of());

        listener.onLocationsCompleted(new MeetingLocationsCompletedEvent(MEETING_ID));

        verify(webPushSender, never()).send(any(), anyString(), anyString(), anyString());
        verify(dispatchService, never()).removeExpired(any());
    }

    @Test
    void 만료된_구독은_발송_후_정리한다() {
        when(dispatchService.collectMeetingParticipantTargets(MEETING_ID)).thenReturn(List.of(TARGET));
        when(webPushSender.send(any(), anyString(), anyString(), anyString())).thenReturn(WebPushSender.Result.EXPIRED);

        listener.onLocationsCompleted(new MeetingLocationsCompletedEvent(MEETING_ID));

        verify(dispatchService).removeExpired(List.of("endpoint-1"));
    }
}
