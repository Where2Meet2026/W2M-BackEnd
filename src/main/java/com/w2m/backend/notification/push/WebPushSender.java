package com.w2m.backend.notification.push;

import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 단일 구독에 Web Push 한 건을 발송한다.
 * notification.push.enabled=false 이거나 VAPID 키가 없으면 실제 발송 없이 로그만 남긴다.
 */
@Slf4j
@Component
public class WebPushSender {

    public enum Result { SENT, EXPIRED, FAILED, SKIPPED }

    private final boolean enabled;
    private final ObjectProvider<PushService> pushServiceProvider;

    public WebPushSender(@Value("${notification.push.enabled}") boolean enabled,
                         ObjectProvider<PushService> pushServiceProvider) {
        this.enabled = enabled;
        this.pushServiceProvider = pushServiceProvider;
    }

    public Result send(PushTarget target, String title, String body, String url) {
        if (!enabled) {
            log.info("[push disabled] {} <- \"{}\" / \"{}\"", target.endpoint(), title, body);
            return Result.SKIPPED;
        }

        PushService pushService = pushServiceProvider.getIfAvailable();
        if (pushService == null) {
            log.warn("[push] enabled=true 이지만 PushService 빈이 없습니다. VAPID 키 설정 확인.");
            return Result.FAILED;
        }

        try {
            Notification notification = new Notification(
                    target.endpoint(),
                    target.p256dh(),
                    target.auth(),
                    toPayload(title, body, url).getBytes(StandardCharsets.UTF_8));

            int status = pushService.send(notification).getStatusLine().getStatusCode();

            if (status == 404 || status == 410) {
                return Result.EXPIRED;
            }
            if (status >= 200 && status < 300) {
                return Result.SENT;
            }
            log.warn("[push] 발송 실패 status={} endpoint={}", status, target.endpoint());
            return Result.FAILED;
        } catch (Exception e) {
            log.warn("[push] 발송 오류 endpoint={} : {}", target.endpoint(), e.toString());
            return Result.FAILED;
        }
    }

    private String toPayload(String title, String body, String url) {
        return "{\"title\":" + jsonString(title)
                + ",\"body\":" + jsonString(body)
                + ",\"url\":" + jsonString(url) + "}";
    }

    private static String jsonString(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder(value.length() + 2).append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }
}
