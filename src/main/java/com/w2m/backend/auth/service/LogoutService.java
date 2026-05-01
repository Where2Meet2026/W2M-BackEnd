package com.w2m.backend.auth.service;

import com.w2m.backend.auth.jwt.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class LogoutService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${SecretKey}")
    private String secretKey;

    public void logout(String accessToken) {
        // 토큰의 남은 유효 시간을 계산
        Long expiration = JwtTokenUtil.getExpiration(accessToken, secretKey);

        // Redis에 토큰을 블랙리스트로 등록하고, 남은 유효 시간만큼만 저장
        if (expiration > 0) {
            redisTemplate.opsForValue().set(accessToken, "logout", expiration, TimeUnit.MILLISECONDS);
        }
    }
}
