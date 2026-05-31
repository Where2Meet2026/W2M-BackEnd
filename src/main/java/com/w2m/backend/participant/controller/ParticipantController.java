package com.w2m.backend.participant.controller;

import com.w2m.backend.auth.jwt.CustomUserDetails;
import com.w2m.backend.participant.dto.request.CreateParticipantRequest;
import com.w2m.backend.participant.dto.response.ParticipantResponse;
import com.w2m.backend.participant.service.ParticipantService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/meetings")
public class ParticipantController {

    private final ParticipantService participantService;

    //초대코드로 모임 참여
    @PostMapping("/join")
    public ParticipantResponse joinMeeting(
            @RequestBody CreateParticipantRequest request) {
        return participantService.joinMeeting(request);
    }
    // 특정 모임 참여자 목록 조회
    @GetMapping("/{meetingId}/participants")
    public List<ParticipantResponse> getParticipants(
            @PathVariable Long meetingId) {
        return participantService.getParticipants(meetingId);
    }
    @DeleteMapping("/{meetingId}/participants/me")
    public void leaveMeeting(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUser().getId();

        participantService.leaveMeeting(meetingId, userId);
    }

}
