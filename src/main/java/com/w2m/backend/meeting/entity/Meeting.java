package com.w2m.backend.meeting.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor @AllArgsConstructor
@Table(name = "meetings")
public class Meeting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 모임을 만든 사용자 ID
    @Column(name = "host_user_id", nullable = false)
    private Long hostUserId;

    //모임 이름
    @Column(nullable = false ,length = 100)
    private String title;

    //모임 설명
    @Column(length = 500)
    private String description;

    //초대 코드
    @Column(name="invite_code", nullable = false, unique = true, length = 20)
    private String inviteCode;

    //현재 모임 진행 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false,length = 50)
    private MeetingStatus status;

    //모임 생성자
    public Meeting(
            Long hostUserId,
            String title,
            String description,
            String inviteCode) {
        this.hostUserId = hostUserId;
        this.title = title;
        this.description = description;
        this.inviteCode = inviteCode;
        this.status = MeetingStatus.WAITING_PARTICIPANTS;
    }

    public void updateStatus(MeetingStatus status){
        this.status = status;
    }

    public enum MeetingStatus {
        WAITING_PARTICIPANTS, //참여자 모집 중
        COLLECTING_TIME, // 시간 입력 중
        TIME_CONFIRMED, // 시간 확정 완료
        COLLECTING_LOCATION, // 위치 입력
        RECOMMENDING,
        RECOMMENDATION_READY, // 추천 생성 중
        VOTING, // 투표 중
        CONFIRMED // 최종 확정
    }
}
