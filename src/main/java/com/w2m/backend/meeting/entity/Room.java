package com.w2m.backend.meeting.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String roomCode; // 공유할 고유 코드

    private String title;    // 방 이름

    private LocalDateTime createdAt;

    private LocalDateTime expiredAt;

    @Builder
    public Room(String roomCode, String title) {
        this.roomCode = roomCode;
        this.title = title;
        this.createdAt = LocalDateTime.now();
    }
}