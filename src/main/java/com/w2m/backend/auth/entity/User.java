package com.w2m.backend.auth.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ✅ 이메일이 곧 로그인 아이디 역할을 합니다.
    @Column(nullable = false, unique = true)
    private String email;

    // LOCAL 유저는 필수, KAKAO 유저는 null 허용
    @Column(nullable = true)
    private String password;

    @Column(nullable = false)
    private String name;

    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Provider provider; // LOCAL, KAKAO

    // 카카오에서 제공하는 고유 식별 번호
    private String providerId;

    @Builder
    public User(String email, String password, String name, String phoneNumber,
                Provider provider, String providerId) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.provider = provider;
        this.providerId = providerId;
    }
}