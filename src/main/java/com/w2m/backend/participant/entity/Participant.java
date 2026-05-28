package com.w2m.backend.participant.entity;

import com.w2m.backend.auth.entity.User;
import com.w2m.backend.meeting.entity.Meeting;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "participants")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Participant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // 로그인 사용자인 경우

    @Column(nullable = false)
    private String nickname;

    private String password; // 비회원 참여자가 자신의 입력을 수정하기 위한 비밀번호

    @Builder
    public Participant(Meeting meeting, User user, String nickname, String password) {
        this.meeting = meeting;
        this.user = user;
        this.nickname = nickname;
        this.password = password;
    }
}
