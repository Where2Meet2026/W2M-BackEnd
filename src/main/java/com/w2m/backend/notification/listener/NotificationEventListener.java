package com.w2m.backend.notification.listener;

import com.w2m.backend.meeting.event.MeetingTimeConfirmedEvent;
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

    private static final String TIME_CONFIRMED_TITLE = "약속 시간이 확정되었습니다";
    private static final String TIME_CONFIRMED_BODY = "출발 위치를 입력해 주세요.";

    private final NotificationDispatchService dispatchService;
    private final WebPushSender webPushSender;

    @Value("${notification.web-base-url}")
    private String webBaseUrl;

    /**
     * 방장이 시간을 확정하면(트랜잭션 커밋 후) 그 모임 참여자 전원에게 푸시.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMeetingTimeConfirmed(MeetingTimeConfirmedEvent event) {
        List<PushTarget> targets = dispatchService.collectMeetingParticipantTargets(event.meetingId());
        if (targets.isEmpty()) {
            return;
        }

        String url = webBaseUrl + "/meetings/" + event.meetingId();
        List<String> expiredEndpoints = new ArrayList<>();

        for (PushTarget target : targets) {
            WebPushSender.Result result =
                    webPushSender.send(target, TIME_CONFIRMED_TITLE, TIME_CONFIRMED_BODY, url);
            if (result == WebPushSender.Result.EXPIRED) {
                expiredEndpoints.add(target.endpoint());
            }
        }

        dispatchService.removeExpired(expiredEndpoints);
        log.info("[push] meeting {} 시간확정 알림 - 대상 {}건, 만료 정리 {}건",
                event.meetingId(), targets.size(), expiredEndpoints.size());
    }
}
