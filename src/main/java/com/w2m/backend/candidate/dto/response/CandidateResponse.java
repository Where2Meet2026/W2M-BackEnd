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
    private String categoryCode;
    private PlaceCandidate.CandidateType type;
    private Double avgDistanceMeters;
    private Integer avgTravelMinutes;
    private Integer maxTravelMinutes;
    private String description;
    private long likeCount;
    private long dislikeCount;
    private CandidateReaction.ReactionType myReaction; // 반응 없으면 null

    public static CandidateResponse of(PlaceCandidate candidate, long likeCount,
   long dislikeCount, CandidateReaction.ReactionType myReaction) {
        return of(candidate, likeCount, dislikeCount, myReaction, candidate.getDescription());
    }

    // 리뷰 기반 AI 설명이 있으면 후보에 저장된 기본 문구 대신 그 설명을 내려준다.
    public static CandidateResponse of(PlaceCandidate candidate, long likeCount,
   long dislikeCount, CandidateReaction.ReactionType myReaction, String description) {
        return CandidateResponse.builder()
                .candidateId(candidate.getId())
                .placeName(candidate.getPlaceName())
                .address(candidate.getAddress())
                .categoryCode(candidate.getCategoryCode())
                .type(candidate.getType())
                .avgDistanceMeters(candidate.getAvgDistanceMeters())
                .avgTravelMinutes(candidate.getAvgTravelMinutes())
                .maxTravelMinutes(candidate.getMaxTravelMinutes())
                .description(candidate.getDescription())
                .description(description)
                .likeCount(likeCount)
                .dislikeCount(dislikeCount)
                .myReaction(myReaction)
                .build();
    }

}
