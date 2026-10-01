package com.w2m.backend.notification.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 알림 리스너를 요청 스레드와 분리해 실행하기 위한 @Async 활성화.
 * 실행기는 Spring Boot가 자동 구성하는 applicationTaskExecutor를 사용한다.
 */
@Configuration
@EnableAsync
public class NotificationAsyncConfig {
}
