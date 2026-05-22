package com.w2m.backend.meeting.controller;

import com.w2m.backend.meeting.dto.request.CreateMeetingRequest;
import com.w2m.backend.meeting.dto.request.UpdateMeetingStatusRequest;
import com.w2m.backend.meeting.dto.response.MeetingResponse;
import com.w2m.backend.meeting.service.MeetingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/meetings")

public class MeetingController {

    private final MeetingService meetingService;

    @PostMapping
    public MeetingResponse createMeeting(@RequestBody CreateMeetingRequest request) {
        return meetingService.createMeeting(request);
    }
    @GetMapping("/{meetingId}")
    public MeetingResponse getMeeting(@PathVariable Long meetingId) {

        return meetingService.getMeeting(meetingId);
    }
    @GetMapping("/my")
    public List<MeetingResponse> getMyMeetings() {

        Long hostUerId  = 1L; //임시 코딩

        return meetingService.getMyMeetings(hostUerId);
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
