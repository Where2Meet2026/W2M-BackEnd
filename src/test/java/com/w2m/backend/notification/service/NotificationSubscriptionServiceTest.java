package com.w2m.backend.notification.service;

import com.w2m.backend.auth.entity.Provider;
import com.w2m.backend.auth.entity.User;
import com.w2m.backend.auth.repository.UserRepository;
import com.w2m.backend.notification.dto.request.SubscribeRequest;
import com.w2m.backend.notification.entity.PushSubscription;
import com.w2m.backend.notification.repository.PushSubscriptionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// 서버/DB 없이 레포지토리를 mock으로 대체하는 순수 단위 테스트.
@ExtendWith(MockitoExtension.class)
class NotificationSubscriptionServiceTest {

    @Mock private PushSubscriptionRepository pushSubscriptionRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private NotificationSubscriptionService notificationSubscriptionService;

    private static final Long USER_ID = 1L;
    private static final String ENDPOINT = "https://fcm.googleapis.com/send/abc123";

    private User userWithId(Long id) {
        User user = User.builder().email("a@a.com").name("석영").provider(Provider.LOCAL).build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private SubscribeRequest subscribeRequest(String endpoint, String p256dh, String auth) {
        SubscribeRequest.Keys keys = new SubscribeRequest.Keys();
        ReflectionTestUtils.setField(keys, "p256dh", p256dh);
        ReflectionTestUtils.setField(keys, "auth", auth);
        SubscribeRequest request = new SubscribeRequest();
        ReflectionTestUtils.setField(request, "endpoint", endpoint);
        ReflectionTestUtils.setField(request, "keys", keys);
        return request;
    }

    @Test
    void 신규_엔드포인트면_새로_저장한다() {
        User user = userWithId(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(pushSubscriptionRepository.findByEndpoint(ENDPOINT)).thenReturn(Optional.empty());

        notificationSubscriptionService.subscribe(USER_ID, subscribeRequest(ENDPOINT, "p256dh-key", "auth-key"));

        ArgumentCaptor<PushSubscription> captor = ArgumentCaptor.forClass(PushSubscription.class);
        verify(pushSubscriptionRepository).save(captor.capture());
        PushSubscription saved = captor.getValue();
        assertThat(saved.getEndpoint()).isEqualTo(ENDPOINT);
        assertThat(saved.getP256dh()).isEqualTo("p256dh-key");
        assertThat(saved.getAuth()).isEqualTo("auth-key");
        assertThat(saved.getUser()).isEqualTo(user);
    }

    @Test
    void 이미_있는_엔드포인트면_새로_저장하지않고_갱신만한다() {
        User oldOwner = userWithId(1L);
        User newOwner = userWithId(2L);
        PushSubscription existing = PushSubscription.builder()
                .user(oldOwner).endpoint(ENDPOINT).p256dh("old-p256dh").auth("old-auth").build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(newOwner));
        when(pushSubscriptionRepository.findByEndpoint(ENDPOINT)).thenReturn(Optional.of(existing));

        notificationSubscriptionService.subscribe(2L, subscribeRequest(ENDPOINT, "new-p256dh", "new-auth"));

        // 같은 endpoint로 재구독 → 새로 저장되는 게 아니라 기존 row가 갱신됨 (다른 사람이 같은 브라우저로 로그인한 경우 포함)
        verify(pushSubscriptionRepository, never()).save(any());
        assertThat(existing.getUser()).isEqualTo(newOwner);
        assertThat(existing.getP256dh()).isEqualTo("new-p256dh");
        assertThat(existing.getAuth()).isEqualTo("new-auth");
    }

    @Test
    void 존재하지_않는_사용자면_예외() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                notificationSubscriptionService.subscribe(USER_ID, subscribeRequest(ENDPOINT, "p", "a")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용자를 찾을 수 없습니다.");

        verify(pushSubscriptionRepository, never()).save(any());
    }

    @Test
    void 본인_구독이면_구독해제된다() {
        User owner = userWithId(USER_ID);
        PushSubscription subscription = PushSubscription.builder()
                .user(owner).endpoint(ENDPOINT).p256dh("p").auth("a").build();
        when(pushSubscriptionRepository.findByEndpoint(ENDPOINT)).thenReturn(Optional.of(subscription));

        notificationSubscriptionService.unsubscribe(USER_ID, ENDPOINT);

        verify(pushSubscriptionRepository).delete(subscription);
    }

    @Test
    void 본인_소유가_아니면_삭제하지_않는다() {
        User someoneElse = userWithId(999L);
        PushSubscription subscription = PushSubscription.builder()
                .user(someoneElse).endpoint(ENDPOINT).p256dh("p").auth("a").build();
        when(pushSubscriptionRepository.findByEndpoint(ENDPOINT)).thenReturn(Optional.of(subscription));

        notificationSubscriptionService.unsubscribe(USER_ID, ENDPOINT);

        verify(pushSubscriptionRepository, never()).delete(any(PushSubscription.class));
    }

    @Test
    void 존재하지_않는_엔드포인트는_조용히_넘어간다_멱등() {
        when(pushSubscriptionRepository.findByEndpoint(ENDPOINT)).thenReturn(Optional.empty());

        // 예외 없이 끝나야 한다 (이미 해제된 구독을 다시 해제해도 에러가 아님)
        notificationSubscriptionService.unsubscribe(USER_ID, ENDPOINT);

        verify(pushSubscriptionRepository, never()).delete(any(PushSubscription.class));
    }
}
