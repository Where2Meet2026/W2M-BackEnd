package com.w2m.backend.notification.dispatch;

import com.w2m.backend.auth.entity.Provider;
import com.w2m.backend.auth.entity.User;
import com.w2m.backend.notification.entity.PushSubscription;
import com.w2m.backend.notification.push.PushTarget;
import com.w2m.backend.notification.repository.PushSubscriptionRepository;
import com.w2m.backend.participant.entity.Participant;
import com.w2m.backend.participant.repository.ParticipantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

// 서버/DB 없이 레포지토리를 mock으로 대체하는 순수 단위 테스트.
@ExtendWith(MockitoExtension.class)
class NotificationDispatchServiceTest {

    @Mock private ParticipantRepository participantRepository;
    @Mock private PushSubscriptionRepository pushSubscriptionRepository;

    @InjectMocks
    private NotificationDispatchService notificationDispatchService;

    private static final Long MEETING_ID = 1L;

    private User userWithId(Long id) {
        User user = User.builder().email("u" + id + "@a.com").name("user" + id).provider(Provider.LOCAL).build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Participant participantOf(User user) {
        return new Participant(null, user, Participant.ParticipantRole.PARTICIPANT);
    }

    @Test
    void 참여자가_없으면_빈_리스트를_반환하고_구독_조회는_안한다() {
        when(participantRepository.findByMeetingId(MEETING_ID)).thenReturn(List.of());

        List<PushTarget> targets = notificationDispatchService.collectMeetingParticipantTargets(MEETING_ID);

        assertThat(targets).isEmpty();
        verify(pushSubscriptionRepository, never()).findByUserIdIn(anyCollection());
    }

    @Test
    void 참여자_전원의_구독_대상을_모으고_유저_중복은_제거한다() {
        User userA = userWithId(10L);
        User userB = userWithId(20L);
        // 같은 유저가 참여자 테이블에 두 번 잡혀도(이론상) userId 기준 중복 제거되어야 함
        when(participantRepository.findByMeetingId(MEETING_ID))
                .thenReturn(List.of(participantOf(userA), participantOf(userB), participantOf(userA)));

        PushSubscription subA = PushSubscription.builder()
                .user(userA).endpoint("endpoint-a").p256dh("p-a").auth("a-a").build();
        PushSubscription subB = PushSubscription.builder()
                .user(userB).endpoint("endpoint-b").p256dh("p-b").auth("a-b").build();

        ArgumentCaptor<Collection<Long>> userIdsCaptor = ArgumentCaptor.forClass(Collection.class);
        when(pushSubscriptionRepository.findByUserIdIn(anyCollection())).thenReturn(List.of(subA, subB));

        List<PushTarget> targets = notificationDispatchService.collectMeetingParticipantTargets(MEETING_ID);

        verify(pushSubscriptionRepository).findByUserIdIn(userIdsCaptor.capture());
        assertThat(userIdsCaptor.getValue()).containsExactlyInAnyOrderElementsOf(Set.of(10L, 20L));

        assertThat(targets).containsExactlyInAnyOrder(
                new PushTarget("endpoint-a", "p-a", "a-a"),
                new PushTarget("endpoint-b", "p-b", "a-b")
        );
    }

    @Test
    void 만료된_구독_엔드포인트를_삭제한다() {
        notificationDispatchService.removeExpired(List.of("expired-1", "expired-2"));

        verify(pushSubscriptionRepository).deleteByEndpointIn(List.of("expired-1", "expired-2"));
    }

    @Test
    void 빈_컬렉션이면_삭제_쿼리를_호출하지_않는다() {
        notificationDispatchService.removeExpired(List.of());

        verify(pushSubscriptionRepository, never()).deleteByEndpointIn(anyCollection());
    }
}
