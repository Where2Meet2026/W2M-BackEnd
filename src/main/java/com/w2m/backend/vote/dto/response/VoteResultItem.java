package com.w2m.backend.vote.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VoteResultItem {
    private Long candidateId;
    private long voteCount; // 이 후보 하나가 받은 표 수 (사람 수가 아니라 "후보 기준" 카운트)

    public static VoteResultItem of(Long candidateId, long voteCount) {
        return VoteResultItem.builder()
                .candidateId(candidateId)
                .voteCount(voteCount)
                .build();
    }
}
