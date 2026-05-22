package com.w2m.backend.meeting.service;


import com.w2m.backend.meeting.dto.request.CreateMeetingRequest;
import com.w2m.backend.meeting.dto.request.UpdateMeetingStatusRequest;
import com.w2m.backend.meeting.dto.response.MeetingResponse;
import com.w2m.backend.meeting.entity.Meeting;
import com.w2m.backend.meeting.repository.MeetingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MeetingService {
    private final MeetingRepository meetingRepository;

    public MeetingResponse createMeeting(CreateMeetingRequest request){

        String inviteCode = UUID.randomUUID()
                .toString()
                .substring(0, 6);

        Meeting meeting = new Meeting(
                1L,
                request.getTitle(),
                request.getDescription(),
                inviteCode
        );
        Meeting savedMeeting = meetingRepository.save(meeting);

        return MeetingResponse.from(savedMeeting);
    }
    public MeetingResponse getMeeting(Long meetingId){

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("모임이 존재하지않습니다."));

        return MeetingResponse.from(meeting);
    }
    public List<MeetingResponse> getMyMeetings(Long hostUserId){
        // GET /api/meetings/my
        List<Meeting> meetings = meetingRepository.findByHostUserId(hostUserId);

        return meetings.stream()
                .map(MeetingResponse::from)
                .toList();
    }
    public MeetingResponse updateMeetingStatus(Long meetingId, UpdateMeetingStatusRequest request){

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow( ()-> new IllegalArgumentException("모임이 존재하지 않습니다."));

        meeting.updateStatus(request.getStatus());

        return MeetingResponse.from(meeting);
    }

    public void deleteMeeting(Long meetingId){

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("모임이 존재하지 않습니다."));

        meetingRepository.delete(meeting);
    }
    public MeetingResponse getMeetingByInviteCode(String inviteCode){
        Meeting meeting = meetingRepository.findByInviteCode(inviteCode)
                .orElseThrow(()-> new IllegalArgumentException("초대 코드에 해당하는 방이 없습니다."));
        return MeetingResponse.from(meeting);
    }
}
