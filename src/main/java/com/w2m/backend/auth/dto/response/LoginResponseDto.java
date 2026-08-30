
package com.w2m.backend.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponseDto {
    private String token;           // JWT 토큰
    private String name;            // 이름
    private String email;           // 이메일
    private String message;         // 응답 메시지 (예: "로그인 성공")

}
