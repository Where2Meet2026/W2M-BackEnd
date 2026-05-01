package com.w2m.backend.auth.service;

import com.w2m.backend.auth.entity.Provider;
import com.w2m.backend.auth.entity.User;
import com.w2m.backend.auth.jwt.CustomUserDetails;
import com.w2m.backend.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 1. 카카오로부터 유저 정보 가져오기
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // 2. 카카오 유저 정보 파싱
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

        String email = (String) kakaoAccount.get("email");
        String name = (String) profile.get("nickname");
        String providerId = String.valueOf(attributes.get("id"));

        // 3. 기존 유저 확인 및 정책 적용
        return userRepository.findByEmail(email)
                .map(existingUser -> {
                    // [Case 1] 동일한 이메일의 일반(LOCAL) 계정이 이미 존재하는 경우
                    if (existingUser.getProvider() == Provider.LOCAL) {
                        throw new OAuth2AuthenticationException("ALREADY_LOCAL_USER");
                    }
                    // [Case 2] 이미 가입된 카카오 계정인 경우 -> 정상 로그인
                    return new CustomUserDetails(existingUser, attributes);
                })
                .orElseGet(() -> {
                    // [Case 3] 신규 소셜 유저 -> DB에 저장하지 않고 임시 객체만 생성하여 반환
                    User tempUser = User.builder()
                            .email(email)
                            .name(name)
                            .provider(Provider.KAKAO)
                            .providerId(providerId)
                            .build();
                    return new CustomUserDetails(tempUser, attributes);
                });
    }
}
