package com.w2m.backend.vote.entity;

import com.w2m.backend.candidate.entity.PlaceCandidate;
import com.w2m.backend.meeting.entity.Meeting;
import com.w2m.backend.participant.entity.Participant;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// "이 모임에서 이 참여자가 이 후보에게 최종 투표했다"를 나타내는 한 줄.
// 좋아요/싫어요(CandidateReaction, 자유롭게 변경 가능한 반응)와는 완전히 별개의, 확정에 쓰이는 투표.
@Entity
@Table(name = "votes", uniqueConstraints = @UniqueConstraint(columnNames = {"meeting_id", "participant_id"})) // 참여자당 모임 하나에 투표는 1개만 — DB 레벨에서 중복 방지
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private PlaceCandidate candidate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id", nullable = false)
    private Participant participant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name ="meeting_id", nullable = false)
    private Meeting meeting;

    @Builder
    public Vote(PlaceCandidate candidate, Participant participant, Meeting meeting) {
        this.candidate = candidate;
        this.participant = participant;
        this.meeting = meeting;
    }
    // 마감 전 다른 후보로 투표를 바꿀 때 사용 — 새 행을 만드는 게 아니라 기존 투표의 후보만 교체
    public void changeCandidate(PlaceCandidate candidate){
        this.candidate = candidate;
    }
}
