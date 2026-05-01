package com.w2m.backend.auth.config;



import com.w2m.backend.auth.jwt.JwtTokenFilter;
import com.w2m.backend.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserService userService;
    @Value("${SecretKey}")
    private String secretKey;
    private final RedisTemplate<String, Object> redisTemplate;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                // 1. 기본 인증 및 CSRF 비활성화 (Stateless 구조)
                .httpBasic(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 2. 접근 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // 로그인을 하지 않아도 접근 가능한 경로 (로그인, 회원가입, 인증 코드 등)
                        .requestMatchers(
                                "/api/v1/user/login",
                                "/api/v1/admin/login",
                                "/api/v1/register",
                                "/api/v1/send-code",
                                "/api/v1/verify-code",
                                "/api/v1/organizer/status"
                        ).permitAll()

                        // 그 외 모든 요청(.anyRequest())은 로그인(authenticated)이 되어야만 가능
                        // 역할(Role) 구분 없이 '인증 여부'만 확인합니다.
                        .anyRequest().authenticated()
                )

                // 3. JWT 필터 배치
                .addFilterBefore(new JwtTokenFilter(userService, secretKey, redisTemplate),
                        UsernamePasswordAuthenticationFilter.class);

        return httpSecurity.build();
    }
}
