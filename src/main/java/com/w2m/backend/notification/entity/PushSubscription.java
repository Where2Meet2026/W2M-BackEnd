package com.w2m.backend.notification.entity;

import com.w2m.backend.auth.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "push_subscriptions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PushSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 브라우저가 발급하는 구독 고유 주소
    @Column(nullable = false, length = 500)
    private String endpoint;

    // Web Push 암호화에 필요한 키 쌍 (VAPID)
    @Column(nullable = false)
    private String p256dh;

    @Column(nullable = false)
    private String auth;

    @Builder
    public PushSubscription(User user, String endpoint, String p256dh, String auth) {
        this.user = user;
        this.endpoint = endpoint;
        this.p256dh = p256dh;
        this.auth = auth;
    }
}
