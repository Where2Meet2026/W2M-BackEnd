package com.w2m.backend.meeting.controller;

import com.w2m.backend.meeting.dto.request.CreateMeetingRequest;
import com.w2m.backend.meeting.dto.request.UpdateMeetingStatusRequest;
import com.w2m.backend.meeting.dto.response.MeetingResponse;
import com.w2m.backend.meeting.service.MeetingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.Authentication;
import com.w2m.backend.auth.jwt.CustomUserDetails;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/meetings")

public class MeetingController {

    private final MeetingService meetingService;

    @PostMapping
    public MeetingResponse createMeeting(@RequestBody CreateMeetingRequest request, Authentication authentication) {
        Long userId = null;
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            userId = ((CustomUserDetails) authentication.getPrincipal()).getId();
        }
        return meetingService.createMeeting(request, userId);
    }
    @GetMapping("/{meetingId}")
    public MeetingResponse getMeeting(@PathVariable Long meetingId) {

        return meetingService.getMeeting(meetingId);
    }
    @GetMapping("/my")
    public List<MeetingResponse> getMyMeetings(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            throw new RuntimeException("인증 정보가 없습니다.");
        }
        
        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getId();

        return meetingService.getMyMeetings(userId);
    }
    @PatchMapping("/{meetingId}/status")
    public MeetingResponse updateMeetingStatus(
            @PathVariable Long meetingId,
            @RequestBody UpdateMeetingStatusRequest request) {

        return meetingService.updateMeetingStatus(meetingId, request);
    }
    @DeleteMapping("/{meetingId}")
    public void deleteMeeting(@PathVariable Long meetingId) {
        meetingService.deleteMeeting(meetingId);
    }
    @GetMapping("/invite/{inviteCode}")
    public MeetingResponse getMeetingByInviteCode(@PathVariable String inviteCode) {
        return meetingService.getMeetingByInviteCode(inviteCode);
    }

}
