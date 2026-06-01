package com.w2m.backend.auth.controller;

import com.w2m.backend.auth.dto.LoginRequestDto;
import com.w2m.backend.auth.dto.LoginResponseDto;
import com.w2m.backend.auth.dto.RegisterRequestDto;
import com.w2m.backend.auth.dto.SocialRegisterRequestDto;
import com.w2m.backend.auth.entity.User;
import com.w2m.backend.auth.jwt.JwtTokenUtil;
import com.w2m.backend.auth.service.LogoutService;
import com.w2m.backend.auth.service.UserService;
import com.w2m.backend.auth.service.VerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class UserController {

    private final UserService userService;
    private final LogoutService logoutService;
    private final VerificationService verificationService;


    @Value("${SecretKey}")
    private String secretKey;

    @PostMapping("/signup")
    public ResponseEntity<String> register(@RequestBody RegisterRequestDto registerRequestDto) {
        // 0. 이메일 인증 여부 확인
        if (!verificationService.isVerified(registerRequestDto.getEmail())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("이메일 인증이 완료되지 않았습니다.");
        }

        // 1. loginId 중복 체크
        if(userService.checkLoginIdDuplicate(registerRequestDto.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("로그인 아이디가 중복됩니다.");
        }

        // 2. 비밀번호 불일치 시 -> 400 Bad Request (실패)
        /*
        if(!registerRequestDto.getPassword().equals(registerRequestDto.getPasswordCheck())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("비밀번호가 일치하지 않습니다.");
        }*/

        // 3. 진짜 성공 시 -> 201 Created (성공)
        userService.join(registerRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body("회원가입 성공");
    }

    @PostMapping("/social-signup")
    public ResponseEntity<LoginResponseDto> socialRegister(@RequestBody SocialRegisterRequestDto socialRegisterRequestDto) {
        // 1. 이메일 중복 체크
        if(userService.checkLoginIdDuplicate(socialRegisterRequestDto.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new LoginResponseDto(null, null, null, "이미 가입된 이메일입니다."));
        }

        // 2. 가입 처리 (이때 DB에 저장)
        User user = userService.socialJoin(socialRegisterRequestDto);

        // 3. 가입 성공 후 즉시 JWT 발급 (자동 로그인 효과)
        long expireTimeMs = 1000 * 60 * 60; // 60분
        String jwtToken = JwtTokenUtil.createToken(user.getId(), secretKey, expireTimeMs);

        LoginResponseDto response = new LoginResponseDto(
                jwtToken,
                user.getName(),
                user.getEmail(),
                "소셜 회원가입 및 로그인 성공"
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @PostMapping("/user/login")
    public ResponseEntity<LoginResponseDto> userLogin(@RequestBody LoginRequestDto loginRequestDto) {
        User user = userService.userLogin(loginRequestDto);

        if (user == null) {
            // 로그인 실패 시
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new LoginResponseDto(null, null, null,"로그인 실패"));
        }

        // 로그인 성공 시 JWT 발급
        long expireTimeMs = 1000 * 60 * 60; // 60분
        String jwtToken = JwtTokenUtil.createToken(user.getId(), secretKey, expireTimeMs);

        // DTO에 필요한 정보만 담기
        LoginResponseDto response = new LoginResponseDto(
                jwtToken,
                user.getName(),
                user.getEmail(),
                "로그인 성공"
        );

        return ResponseEntity.ok(response);
    }



    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader("Authorization") String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body("토큰이 유효하지 않거나 누락되었습니다.");
        }

        // "Bearer " 부분을 제외하고 실제 토큰 문자열만 추출
        String token = authorizationHeader.substring(7);

        // LogoutService를 호출하여 토큰을 블랙리스트에 추가
        logoutService.logout(token);

        return ResponseEntity.ok("로그아웃 성공");
    }
}
