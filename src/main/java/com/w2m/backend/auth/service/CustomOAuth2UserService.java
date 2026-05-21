package com.w2m.backend.auth.service;

import com.w2m.backend.auth.entity.Provider;
import com.w2m.backend.auth.entity.User;
import com.w2m.backend.auth.jwt.CustomUserDetails;
import com.w2m.backend.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String kakaoClientId;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        log.info("========== 카카오 로그인 시도 시작 ==========");
        log.info("현재 주입된 Kakao Client ID: {}", kakaoClientId);
        
        OAuth2User oAuth2User;
        try {
            oAuth2User = super.loadUser(userRequest);
        } catch (Exception e) {
            log.error("카카오 API로부터 유저 정보를 가져오는 데 실패했습니다.", e);
            throw new OAuth2AuthenticationException("KAKAO_API_ERROR");
        }

        Map<String, Object> attributes = oAuth2User.getAttributes();
        log.info("카카오 응답 데이터: {}", attributes);

        try {
            Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

            String email = (String) kakaoAccount.get("email");
            String name = (String) profile.get("nickname");
            String providerId = String.valueOf(attributes.get("id"));

            log.info("파싱 결과 -> 이메일: {}, 이름: {}, ProviderId: {}", email, name, providerId);

            if (email == null) {
                throw new OAuth2AuthenticationException("EMAIL_NOT_FOUND");
            }

            // [통합 로그인 및 자동 회원가입 정책]
            return userRepository.findByEmail(email)
                    .map(existingUser -> {
                        log.info("기존 유저 발견 ({}): 로그인을 진행합니다.", email);
                        return new CustomUserDetails(existingUser, attributes);
                    })
                    .orElseGet(() -> {
                        log.info("신규 카카오 유저 발견: 즉시 회원가입을 진행합니다.");
                        User newUser = User.builder()
                                .email(email)
                                .name(name)
                                .provider(Provider.KAKAO)
                                .providerId(providerId)
                                .build();
                        // ✅ 즉시 DB 저장
                        User savedUser = userRepository.save(newUser);
                        log.info("신규 유저 DB 저장 완료 (ID: {})", savedUser.getId());
                        return new CustomUserDetails(savedUser, attributes);
                    });
                    
        } catch (Exception e) {
            log.error("데이터 처리 중 에러 발생: ", e);
            throw new OAuth2AuthenticationException("DATA_PARSING_ERROR");
        }
    }
}
