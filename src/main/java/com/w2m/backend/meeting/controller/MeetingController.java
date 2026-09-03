package com.w2m.backend.meeting.controller;

import com.w2m.backend.auth.jwt.CustomUserDetails;
import com.w2m.backend.meeting.dto.request.ConfirmMeetingTimeRequest;
import com.w2m.backend.meeting.dto.request.CreateMeetingRequest;
import com.w2m.backend.meeting.dto.request.UpdateMeetingStatusRequest;
import com.w2m.backend.meeting.dto.response.MeetingResponse;
import com.w2m.backend.meeting.service.MeetingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/meetings")

public class MeetingController {

    private final MeetingService meetingService;

    @PostMapping
    public MeetingResponse createMeeting(
            @RequestBody CreateMeetingRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUser().getId();
        return meetingService.createMeeting(request,userId);
    }
    @GetMapping("/{meetingId}")
    public MeetingResponse getMeeting(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUser().getId();
        return meetingService.getMeeting(meetingId, userId);
    }
    @GetMapping("/my")
    public List<MeetingResponse> getMyMeetings(
            @AuthenticationPrincipal CustomUserDetails userDetails
            ) {
        Long userId  = userDetails.getUser().getId();

        return meetingService.getMyMeetings(userId);
    }
    @PatchMapping("/{meetingId}/status")
    public MeetingResponse updateMeetingStatus(
            @PathVariable Long meetingId,
            @RequestBody UpdateMeetingStatusRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getUser().getId();
        return meetingService.updateMeetingStatus(meetingId, request, userId);
    }
    @DeleteMapping("/{meetingId}")
    public void deleteMeeting(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUser().getId();
        meetingService.deleteMeeting(meetingId, userId);
    }
    @GetMapping("/invite/{inviteCode}")
    public MeetingResponse getMeetingByInviteCode(@PathVariable String inviteCode) {
        return meetingService.getMeetingByInviteCode(inviteCode);
    }
    @PatchMapping("/{meetingId}/confirmed-time")
    public MeetingResponse confirmMeetingTime(
            @PathVariable Long meetingId,
            @RequestBody ConfirmMeetingTimeRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUser().getId();
        return meetingService.confirmMeetingTime(meetingId, request, userId);
    }

}
