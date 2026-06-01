package com.w2m.backend.meeting.service;


import com.w2m.backend.meeting.dto.request.CreateMeetingRequest;
import com.w2m.backend.meeting.dto.request.UpdateMeetingStatusRequest;
import com.w2m.backend.meeting.dto.response.MeetingResponse;
import com.w2m.backend.meeting.entity.Meeting;
import com.w2m.backend.meeting.repository.MeetingRepository;
import com.w2m.backend.participant.entity.Participant;
import com.w2m.backend.participant.repository.ParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
@Service
@RequiredArgsConstructor
public class MeetingService {
    private final ParticipantRepository participantRepository;
    private final MeetingRepository meetingRepository;
    private final com.w2m.backend.auth.repository.UserRepository userRepository;
    private final com.w2m.backend.availability.repository.AvailabilityRepository availabilityRepository;

    public MeetingResponse createMeeting(
            CreateMeetingRequest request, Long userId) {

        String inviteCode = UUID.randomUUID()
                .toString()
                .substring(0, 6);

        Meeting meeting = new Meeting(
                userId,
                request.getTitle(),
                request.getDescription(),
                inviteCode
        );
        Meeting savedMeeting = meetingRepository.save(meeting);

        com.w2m.backend.auth.entity.User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Participant hostParticipant = new Participant(
                savedMeeting,
                user,
                Participant.ParticipantRole.HOST
        );
        participantRepository.save(hostParticipant);

        return MeetingResponse.from(savedMeeting, "HOST");
    }
    public MeetingResponse getMeeting(Long meetingId){

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("모임이 존재하지않습니다."));

        return MeetingResponse.from(meeting);
    }
    @Transactional(readOnly = true)

    public List<MeetingResponse> getMyMeetings(Long userId) {
        List<MeetingResponse> result = new ArrayList<>();
        // 내가 방장인 방 조회
        List<Meeting> hostMeetings = meetingRepository.findByHostUserId(userId);
        for (Meeting meeting : hostMeetings) {
            result.add(MeetingResponse.from(meeting, "HOST"));
        }
        // 내가 참여한 방 조회
        List<Participant> participants = participantRepository.findByUserId(userId);
        for (Participant participant : participants) {
            Meeting meeting = participant.getMeeting();
            boolean alreadyAddedAsHost = hostMeetings.stream()
                    .anyMatch(hostMeeting -> hostMeeting.getId().equals(meeting.getId()));
            if (!alreadyAddedAsHost) {
                result.add(MeetingResponse.from(meeting, "PARTICIPANT"));
            }
        }
        return result;

    }
    public MeetingResponse updateMeetingStatus(Long meetingId, UpdateMeetingStatusRequest request){

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow( ()-> new IllegalArgumentException("모임이 존재하지 않습니다."));

        meeting.updateStatus(request.getStatus());

        return MeetingResponse.from(meeting);
    }

    @Transactional
    public void deleteMeeting(Long meetingId, Long userId){
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("모임이 존재하지 않습니다."));
        if (!meeting.getHostUserId().equals(userId)) {
            throw new IllegalArgumentException("방장만 모임을 삭제할 수 있습니다.");
        }
        
        // CascadeType.ALL 설정으로 인해 meeting 삭제 시 
        // 하위 participants와 그 하위 availabilities가 모두 자동 삭제됩니다.
        meetingRepository.delete(meeting);
    }
    public MeetingResponse getMeetingByInviteCode(String inviteCode){
        Meeting meeting = meetingRepository.findByInviteCode(inviteCode)
                .orElseThrow(()-> new IllegalArgumentException("초대 코드에 해당하는 방이 없습니다."));
        return MeetingResponse.from(meeting);
    }
}
