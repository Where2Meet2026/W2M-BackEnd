package com.w2m.backend.auth.dto;

import com.w2m.backend.auth.entity.Provider;
import com.w2m.backend.auth.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SocialRegisterRequestDto {

    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String email;

    @NotBlank(message = "이름은 필수입니다.")
    private String name;

    @NotBlank(message = "제공자 식별 번호는 필수입니다.")
    private String providerId;

    @NotBlank(message = "전화번호는 필수입니다.")
    private String phoneNumber;

    public User toEntity() {
        return User.builder()
                .email(email)
                .name(name)
                .provider(Provider.KAKAO)
                .providerId(providerId)
                .phoneNumber(phoneNumber)
                .build();
    }
}
