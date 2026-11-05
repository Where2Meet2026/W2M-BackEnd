package com.w2m.backend.notification.listener;

import com.w2m.backend.meeting.event.MeetingLocationsCompletedEvent;
import com.w2m.backend.meeting.event.MeetingPlaceConfirmedEvent;
import com.w2m.backend.meeting.event.MeetingTimeConfirmedEvent;
import com.w2m.backend.meeting.event.MeetingTimeInputCompletedEvent;
import com.w2m.backend.notification.dispatch.NotificationDispatchService;
import com.w2m.backend.notification.push.PushTarget;
import com.w2m.backend.notification.push.WebPushSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    // 모임 이름이 있으면 제목 앞에 붙인다. 예) [마포구 주민들] 시간이 확정되었습니다
    private static final String TIME_INPUT_COMPLETED_TITLE = "모든 참가자들의 시간 입력이 완료되었습니다";
    private static final String TIME_CONFIRM_REQUEST_BODY = "시간을 확정해주세요.";
    private static final String TIME_CONFIRMED_TITLE = "시간이 확정되었습니다";
    private static final String LOCATION_REQUEST_BODY = "각 참가자는 출발위치를 입력해주세요.";
    private static final String LOCATIONS_COMPLETED_TITLE = "모두 출발 위치를 입력했어요";
    private static final String VOTE_REQUEST_BODY = "추천 장소를 확인하고 투표해 주세요.";
    private static final String PLACE_CONFIRMED_TITLE = "약속 장소가 확정되었습니다";
    private static final String PLACE_CONFIRMED_FALLBACK_BODY = "확정된 약속을 확인해 주세요.";

    private final NotificationDispatchService dispatchService;
    private final WebPushSender webPushSender;

    @Value("${notification.web-base-url}")
    private String webBaseUrl;

    /**
     * 방장을 뺀 참여자 전원이 시간을 입력하면(트랜잭션 커밋 후) 방장에게만 시간 확정을 요청한다.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTimeInputCompleted(MeetingTimeInputCompletedEvent event) {
        send(event.meetingId(), dispatchService.collectHostTargets(event.meetingId()),
                TIME_INPUT_COMPLETED_TITLE, TIME_CONFIRM_REQUEST_BODY, "/recommendation/", "시간입력완료");
    }

    /**
     * 방장이 시간을 확정하면(트랜잭션 커밋 후) 참여자 전원에게 푸시.
     * 시간 확정과 동시에 위치 입력 단계가 시작되므로, 출발 위치 입력을 함께 요청한다.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMeetingTimeConfirmed(MeetingTimeConfirmedEvent event) {
        sendToParticipants(event.meetingId(), TIME_CONFIRMED_TITLE, LOCATION_REQUEST_BODY, "/location/", "시간확정");
    }

    /**
     * 마지막 참여자가 출발 위치를 입력하면(트랜잭션 커밋 후) 참여자 전원에게 투표를 안내한다.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLocationsCompleted(MeetingLocationsCompletedEvent event) {
        sendToParticipants(event.meetingId(), LOCATIONS_COMPLETED_TITLE, VOTE_REQUEST_BODY, "/candidates/", "투표안내");
    }

    /**
     * 전원 투표로 장소가 확정되면(트랜잭션 커밋 후) 참여자 전원에게 확정 장소를 알린다.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPlaceConfirmed(MeetingPlaceConfirmedEvent event) {
        String placeName = dispatchService.findConfirmedPlaceName(event.meetingId());
        String body = placeName == null ? PLACE_CONFIRMED_FALLBACK_BODY : placeName + "에서 만나요.";
        sendToParticipants(event.meetingId(), PLACE_CONFIRMED_TITLE, body, "/result/", "장소확정");
    }

    private void sendToParticipants(Long meetingId, String title, String body, String path, String label) {
        send(meetingId, dispatchService.collectMeetingParticipantTargets(meetingId), title, body, path, label);
    }

    private void send(Long meetingId, List<PushTarget> targets, String title, String body, String path, String label) {
        if (targets.isEmpty()) {
            return;
        }

        String meetingTitle = dispatchService.findMeetingTitle(meetingId);
        String fullTitle = meetingTitle == null ? title : "[" + meetingTitle + "] " + title;
        String url = webBaseUrl + path + meetingId;
        List<String> expiredEndpoints = new ArrayList<>();

        for (PushTarget target : targets) {
            WebPushSender.Result result = webPushSender.send(target, fullTitle, body, url);
            if (result == WebPushSender.Result.EXPIRED) {
                expiredEndpoints.add(target.endpoint());
            }
        }

        dispatchService.removeExpired(expiredEndpoints);
        log.info("[push] meeting {} {} 알림 - 대상 {}건, 만료 정리 {}건",
                meetingId, label, targets.size(), expiredEndpoints.size());
    }
}
