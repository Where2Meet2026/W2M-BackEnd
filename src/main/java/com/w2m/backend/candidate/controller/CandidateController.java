package com.w2m.backend.candidate.controller;

import com.w2m.backend.auth.jwt.CustomUserDetails;
import com.w2m.backend.candidate.dto.request.ReactionRequest;
import com.w2m.backend.candidate.dto.response.CandidateResponse;
import com.w2m.backend.candidate.service.PlaceCandidateService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/meetings/{meetingId}/candidates")
public class CandidateController {

    private final PlaceCandidateService placeCandidateService;

    @GetMapping
    public List<CandidateResponse> getCandidates(
            @PathVariable Long meetingId , @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUser().getId();
        return placeCandidateService.getCandidates(meetingId, userId);
    }

    @PostMapping("/{candidateId}/reactions")
    public void react(
            @PathVariable Long meetingId,
            @PathVariable Long candidateId,
            @RequestBody ReactionRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails){
        Long userId = userDetails.getUser().getId();
        placeCandidateService.toggleReaction(meetingId, candidateId, userId,
    request.getReactionType());
    }


}
