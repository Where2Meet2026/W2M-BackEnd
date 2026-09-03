package com.w2m.backend.participant.entity;

import com.w2m.backend.auth.entity.User;
import com.w2m.backend.availability.entity.Availability;
import com.w2m.backend.meeting.entity.Meeting;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name= "participants")
public class Participant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ... (existing fields)

    // 가용 시간 목록 (양방향 연관관계 및 영속성 전이 설정)
    @OneToMany(mappedBy = "participant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Availability> availabilities = new ArrayList<>();

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

    // 시간 입력 완료 여부
    @Column(name = "is_time_selected", nullable = false)
    private boolean isTimeSelected = false;

    // 참여자 생성용 생성자
    public Participant(Meeting meeting, User user, ParticipantRole role) {
        this.meeting = meeting;
        this.user = user;
        this.role = role;
        this.isTimeSelected = false;
    }

    public void updateTimeSelected(boolean isTimeSelected) {
        this.isTimeSelected = isTimeSelected;
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
