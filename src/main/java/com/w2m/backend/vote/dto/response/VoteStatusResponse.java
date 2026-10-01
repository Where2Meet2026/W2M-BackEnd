package com.w2m.backend.vote.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class VoteStatusResponse {
    private long totalParticipants; // 이 모임 참여자 총원
    private long votedCount; // 지금까지 투표를 마친 사람 수 ("사람 기준" — voteCount와 다름)
    private boolean isClosed; // 전원 투표 완료되어 확정됐는지
    private Long myVoteCandidateId; // 내가 투표한 후보 id (아직 투표 전이면 null)
    private Long confirmedCandidateId; // 자동 확정된 후보 id (마감 전이면 null)
    private List<VoteResultItem> results; // 후보별 득표수 목록

    public static VoteStatusResponse of(long totalParticipants, long votedCount, boolean isClosed,
                                        Long myVoteCandidateId, Long confirmedCandidateId, List<VoteResultItem> results) {
        return VoteStatusResponse.builder()
                .totalParticipants(totalParticipants)
                .votedCount(votedCount)
                .isClosed(isClosed)
                .myVoteCandidateId(myVoteCandidateId)
                .confirmedCandidateId(confirmedCandidateId)
                .results(results).build();
    }
}
