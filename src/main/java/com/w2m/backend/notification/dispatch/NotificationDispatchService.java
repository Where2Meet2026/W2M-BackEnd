package com.w2m.backend.notification.dispatch;

import com.w2m.backend.candidate.entity.PlaceCandidate;
import com.w2m.backend.meeting.entity.Meeting;
import com.w2m.backend.notification.push.PushTarget;
import com.w2m.backend.notification.repository.PushSubscriptionRepository;
import com.w2m.backend.meeting.repository.MeetingRepository;
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
    private final MeetingRepository meetingRepository;

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

    /** 모임 방장의 Web Push 구독 대상 목록. 모임이 없으면 빈 목록. */
    @Transactional(readOnly = true)
    public List<PushTarget> collectHostTargets(Long meetingId) {
        return meetingRepository.findById(meetingId)
                .map(meeting -> pushSubscriptionRepository.findByUserIdIn(List.of(meeting.getHostUserId())).stream()
                        .map(PushTarget::from)
                        .toList())
                .orElse(List.of());
    }

    /** 알림 문구에 넣을 모임 이름. 모임이 없거나 이름이 비어 있으면 null. */
    @Transactional(readOnly = true)
    public String findMeetingTitle(Long meetingId) {
        return meetingRepository.findById(meetingId)
                .map(meeting -> meeting.getTitle())
                .filter(title -> !title.isBlank())
                .orElse(null);
    }

    /** 투표로 확정된 장소 이름. 모임이 없거나 아직 확정 전이면 null. */
    @Transactional(readOnly = true)
    public String findConfirmedPlaceName(Long meetingId) {
        return meetingRepository.findById(meetingId)
                .map(Meeting::getConfirmedCandidate)
                .map(PlaceCandidate::getPlaceName)
                .orElse(null);
    }

    /** 발송 중 만료(404/410)로 확인된 구독 제거. */
    @Transactional
    public void removeExpired(Collection<String> endpoints) {
        if (!endpoints.isEmpty()) {
            pushSubscriptionRepository.deleteByEndpointIn(endpoints);
        }
    }
}
