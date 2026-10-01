package com.w2m.backend.vote.controller;

import com.w2m.backend.auth.jwt.CustomUserDetails;
import com.w2m.backend.vote.dto.request.VoteRequest;
import com.w2m.backend.vote.dto.response.VoteResponse;
import com.w2m.backend.vote.dto.response.VoteStatusResponse;
import com.w2m.backend.vote.service.VoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor // service 생성자
@RequestMapping("/api/meetings/{meetingId}/votes")

public class VoteController {

    private final VoteService voteService;

    // POST /api/meetings/{meetingId}/votes — 후보 하나에 최종 투표(또는 변경)
    @PostMapping
    public VoteResponse castVote(
            @PathVariable Long meetingId,
            @RequestBody VoteRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ){
        Long userId = userDetails.getUser().getId();
        boolean isClosed = voteService.castVote(meetingId, request.getCandidateId(),userId);

        return VoteResponse.of(request.getCandidateId(), isClosed);
    }
    // GET /api/meetings/{meetingId}/votes — 투표 현황 조회 (득표수, 마감 여부, 내 투표, 확정 후보)
    @GetMapping
    public VoteStatusResponse getVoteStatus(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ){
        Long userId = userDetails.getUser().getId();
        return voteService.getVoteStatus(meetingId, userId);
    }
}
