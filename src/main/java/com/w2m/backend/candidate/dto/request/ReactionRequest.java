package com.w2m.backend.candidate.dto.request;

import com.w2m.backend.candidate.entity.CandidateReaction;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReactionRequest {
    private CandidateReaction.ReactionType reactionType;
}
