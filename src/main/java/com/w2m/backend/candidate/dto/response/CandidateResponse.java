package com.w2m.backend.candidate.dto.response;


import com.w2m.backend.candidate.entity.CandidateReaction;
import com.w2m.backend.candidate.entity.PlaceCandidate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CandidateResponse {

    private Long candidateId;
    private String placeName;
    private String address;
    private PlaceCandidate.CandidateType type;
    private Double avgDistanceMeters;
    private String description;
    private Long likeCount;
    private long dislikeCount;
    private CandidateReaction.ReactionType myReaction; // 반응 없으면 null

    public static CandidateResponse of(PlaceCandidate candidate, long likeCount,
   long dislikeCount, CandidateReaction.ReactionType myReaction) {
        return CandidateResponse.builder()
                .candidateId(candidate.getId())
                .placeName(candidate.getPlaceName())
                .address(candidate.getAddress())
                .type(candidate.getType())
                .avgDistanceMeters(candidate.getAvgDistanceMeters())
                .description(candidate.getDescription())
                .likeCount(likeCount)
                .dislikeCount(dislikeCount)
                .myReaction(myReaction)
                .build();
    }

}
