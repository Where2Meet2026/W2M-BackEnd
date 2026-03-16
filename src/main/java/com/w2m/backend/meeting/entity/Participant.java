package com.w2m.backend.meeting.entity;

import com.w2m.backend.meeting.entity.Room;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_room_nickname",
                columnNames = {"room_id", "nickname"} // 핵심: 한 방 안에서만 닉네임 중복 금지
        )
})
public class Participant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room; // 어떤 방에 참여 중인지

    @Column(nullable = false)
    private String nickname; // 사용자가 입력한 이름

    @Column(nullable = false)
    private String password;

    private LocalDateTime joinedAt;

}
