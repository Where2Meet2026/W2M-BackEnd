package com.w2m.backend.vote.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class VoteRequest {
    private Long candidateId; // POST /votes 요청 body — 이번에 투표할 후보 id
}
