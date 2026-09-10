package com.w2m.backend.notification.service;

import com.w2m.backend.auth.entity.User;
import com.w2m.backend.auth.repository.UserRepository;
import com.w2m.backend.notification.dto.request.SubscribeRequest;
import com.w2m.backend.notification.entity.PushSubscription;
import com.w2m.backend.notification.repository.PushSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationSubscriptionService {

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final UserRepository userRepository;

    /**
     * Web Push 구독 등록. 같은 endpoint가 이미 있으면 키/소유자만 갱신한다(upsert).
     */
    @Transactional
    public void subscribe(Long userId, SubscribeRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        String p256dh = request.getKeys().getP256dh();
        String auth = request.getKeys().getAuth();

        pushSubscriptionRepository.findByEndpoint(request.getEndpoint())
                .ifPresentOrElse(
                        sub -> sub.update(user, p256dh, auth),
                        () -> pushSubscriptionRepository.save(PushSubscription.builder()
                                .user(user)
                                .endpoint(request.getEndpoint())
                                .p256dh(p256dh)
                                .auth(auth)
                                .build())
                );
    }

    /**
     * Web Push 구독 해제. 본인 소유의 구독만 삭제하며, 없어도 정상 처리(멱등).
     */
    @Transactional
    public void unsubscribe(Long userId, String endpoint) {
        pushSubscriptionRepository.findByEndpoint(endpoint)
                .filter(sub -> sub.getUser().getId().equals(userId))
                .ifPresent(pushSubscriptionRepository::delete);
    }
}
