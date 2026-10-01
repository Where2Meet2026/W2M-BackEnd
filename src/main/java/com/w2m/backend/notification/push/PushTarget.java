package com.w2m.backend.notification.push;

import com.w2m.backend.notification.entity.PushSubscription;

/**
 * 발송 시점에 필요한 구독 정보만 담은 불변 값 (트랜잭션 종료 후에도 안전하게 사용).
 */
public record PushTarget(String endpoint, String p256dh, String auth) {

    public static PushTarget from(PushSubscription subscription) {
        return new PushTarget(subscription.getEndpoint(), subscription.getP256dh(), subscription.getAuth());
    }
}
