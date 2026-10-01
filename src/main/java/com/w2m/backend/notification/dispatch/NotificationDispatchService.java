package com.w2m.backend.notification.dispatch;

import com.w2m.backend.notification.push.PushTarget;
import com.w2m.backend.notification.repository.PushSubscriptionRepository;
import com.w2m.backend.participant.repository.ParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * 알림 발송에 필요한 조회/정리를 담당. 리스너(@Async)에서 호출되므로
 * 각 메서드가 자체 트랜잭션을 연다.
 */
@Service
@RequiredArgsConstructor
public class NotificationDispatchService {

    private final ParticipantRepository participantRepository;
    private final PushSubscriptionRepository pushSubscriptionRepository;

    /** 모임 참여자 전원의 Web Push 구독 대상 목록. */
    @Transactional(readOnly = true)
    public List<PushTarget> collectMeetingParticipantTargets(Long meetingId) {
        List<Long> userIds = participantRepository.findByMeetingId(meetingId).stream()
                .map(participant -> participant.getUser().getId())
                .distinct()
                .toList();
        if (userIds.isEmpty()) {
            return List.of();
        }
        return pushSubscriptionRepository.findByUserIdIn(userIds).stream()
                .map(PushTarget::from)
                .toList();
    }

    /** 발송 중 만료(404/410)로 확인된 구독 제거. */
    @Transactional
    public void removeExpired(Collection<String> endpoints) {
        if (!endpoints.isEmpty()) {
            pushSubscriptionRepository.deleteByEndpointIn(endpoints);
        }
    }
}
