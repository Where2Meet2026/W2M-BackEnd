package com.w2m.backend.vote.dto.response;

import com.w2m.backend.vote.entity.Vote;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VoteResponse {
    private Long candidateId; // 내가 이번에 투표한 후보
    private boolean isClosed; // 이 투표로 전원 완료되어 마감·확정됐는지

    public static VoteResponse of(Long candidateId, boolean isClosed) {
        return VoteResponse.builder()
                .candidateId(candidateId)
                .isClosed(isClosed)
                .build();
    }
}
