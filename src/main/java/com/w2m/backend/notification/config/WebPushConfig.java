package com.w2m.backend.notification.config;

import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.GeneralSecurityException;
import java.security.Security;

@Slf4j
@Configuration
public class WebPushConfig {

    /**
     * notification.push.enabled=true 이고 VAPID 키가 채워져 있을 때만 실제 PushService를 만든다.
     * 키가 비어 있으면 null 빈으로 두어 WebPushSender가 로그만 남기도록 한다(앱 기동은 정상).
     */
    @Bean
    @ConditionalOnProperty(name = "notification.push.enabled", havingValue = "true")
    public PushService pushService(
            @Value("${notification.vapid.public-key}") String publicKey,
            @Value("${notification.vapid.private-key}") String privateKey,
            @Value("${notification.vapid.subject}") String subject
    ) {
        if (publicKey == null || publicKey.isBlank() || privateKey == null || privateKey.isBlank()) {
            log.warn("[push] VAPID 키가 비어 있어 실제 발송을 비활성화합니다. (application-local.yml 확인)");
            return null;
        }
        // web-push는 "BC" 프로바이더가 등록돼 있어야 EC 키를 로드할 수 있음
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        try {
            return new PushService(publicKey, privateKey, subject);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("VAPID 키 로딩 실패 - 형식을 확인하세요.", e);
        }
    }
}
