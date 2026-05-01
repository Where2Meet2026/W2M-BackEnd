package com.w2m.backend.auth.service;

import com.w2m.backend.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class VerificationService {

    private final StringRedisTemplate redis;
    private final EmailService emailService;
    private final UserRepository userRepository;

    @Value("${app.verify.code-ttl-sec:600}")
    private int codeTtlSec;

    @Value("${app.verify.prefix:email:verify:}")
    private String prefix;

    private static final SecureRandom RND = new SecureRandom();

    private String keyCode(String email) { return prefix + "code:" + email; }
    private String keyOk(String email)   { return prefix + "ok:"   + email; }

    private String generateCode() {
        int n = RND.nextInt(900000) + 100000; // 100000~999999
        return String.valueOf(n);
    }


    //이메일 중복체크
    public boolean checkEmailDuplicate(String email) {
        return userRepository.existsByEmail(email);
    }

    /** 코드 생성 & 메일 발송 */
    public void sendCode(String email) {
        String code = generateCode();
        redis.opsForValue().set(keyCode(email), code, Duration.ofSeconds(codeTtlSec));
        emailService.sendVerificationCode(email, code, codeTtlSec / 60);
    }

    /** 코드 검증 (성공 시 OK 플래그 세팅) */
    public boolean verifyCode(String email, String inputCode) {
        String saved = redis.opsForValue().get(keyCode(email));
        if (saved == null) return false;
        if (!saved.equals(inputCode)) return false;

        // 코드 1회성 소모(선택): 지우고 OK 플래그로 전환
        redis.delete(keyCode(email));
        redis.opsForValue().set(keyOk(email), "OK", Duration.ofMinutes(30));
        return true;
    }

    /** 가입 시 검증 여부 확인 */
    public boolean isVerified(String email) {
        return "OK".equals(redis.opsForValue().get(keyOk(email)));
    }

    /** 가입 완료 후 흔적 제거(선택) */
    public void clear(String email) {
        redis.delete(keyOk(email));
    }
}
