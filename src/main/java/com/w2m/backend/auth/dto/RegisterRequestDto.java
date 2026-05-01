package com.w2m.backend.auth.dto;


import com.w2m.backend.auth.entity.Provider;
import com.w2m.backend.auth.entity.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RegisterRequestDto {

    private String email;         // 로그인 아이디 겸 이메일
    private String password;
    private String passwordCheck; // 비밀번호 확인용 (DB 저장 X)
    private String name;
    private String phoneNumber;


    //비밀번호 단방향 해시
    public User toEntity(String encodedPassword) {
        return User.builder()
                .email(this.email)
                .password(encodedPassword)
                .name(this.name)
                .phoneNumber(this.phoneNumber)
                .provider(Provider.LOCAL) // 일반 회원가입이므로 LOCAL 고정
                .build();
    }
}
