package com.w2m.backend.participant.entity;

import com.w2m.backend.auth.entity.User;
import com.w2m.backend.meeting.entity.Meeting;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name= "participants")
public class Participant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 모임에 참여했는지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    // 어떤 유저가 참여했는지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 방장인지 게스트 인지
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ParticipantRole role;

    // 참여 시간
    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    // 참여자 생성용 생성자
    public Participant(Meeting meeting, User user, ParticipantRole role) {
        this.meeting = meeting;
        this.user = user;
        this.role = role;
    }
    // DB 저장 직전에 자동 실행
    @PrePersist
    public void onCreate() {
        this.joinedAt = LocalDateTime.now();
    }

    public enum ParticipantRole {
        HOST,
        PARTICIPANT
    }

}
