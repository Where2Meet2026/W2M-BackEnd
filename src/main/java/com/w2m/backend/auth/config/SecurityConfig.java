package com.w2m.backend.auth.config;

import com.w2m.backend.auth.handler.OAuth2SuccessHandler;
import com.w2m.backend.auth.jwt.JwtTokenFilter;
import com.w2m.backend.auth.service.CustomOAuth2UserService;
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
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    @Value("${SecretKey}")
    private String secretKey;
    private final RedisTemplate<String, Object> redisTemplate;
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
    httpSecurity
            // 1. 기본 인증 및 CSRF 비활성화
            .httpBasic(AbstractHttpConfigurer::disable)
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // OAuth2 인증 과정에서 세션(State 저장)이 필요하므로 IF_REQUIRED로 변경
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

            // 2. 접근 권한 설정
            .authorizeHttpRequests(auth -> auth
                    // 로그인을 하지 않아도 접근 가능한 경로
                    .requestMatchers(
                            "/api/auth/user/login",
                            "/api/auth/signup",
                            "/api/v1/send-code",
                            "/api/v1/verify-code",
                            "/api/v1/status",
                            "/login/oauth2/**",
                            "/oauth2/**",
                            "/api/rooms/**",
                            "/swagger-ui/**",
                            "/v3/api-docs/**"

                    ).permitAll()

                    // 그 외 모든 요청은 로그인(authenticated)이 되어야 함
                    .anyRequest().authenticated()
            )

            // 3. OAuth2 로그인 설정
            .oauth2Login(oauth2 -> oauth2
                    .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                    .successHandler(oAuth2SuccessHandler)
            )

            // 4. JWT 필터 배치
            .addFilterBefore(new JwtTokenFilter(userService, secretKey, redisTemplate),
                    UsernamePasswordAuthenticationFilter.class);

    return httpSecurity.build();
}

@Bean
public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
    org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();
    configuration.addAllowedOrigin("http://localhost:5173"); // 프론트엔드 주소
    configuration.addAllowedMethod("*");
    configuration.addAllowedHeader("*");
    configuration.setAllowCredentials(true);
    org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
}
