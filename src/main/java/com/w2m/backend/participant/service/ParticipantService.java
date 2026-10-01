package com.w2m.backend.participant.service;

import com.w2m.backend.auth.entity.User;
import com.w2m.backend.auth.repository.UserRepository;
import com.w2m.backend.candidate.repository.CandidateReactionRepository;
import com.w2m.backend.meeting.entity.Meeting;
import com.w2m.backend.meeting.repository.MeetingRepository;
import com.w2m.backend.participant.dto.request.CreateParticipantRequest;
import com.w2m.backend.participant.dto.response.ParticipantResponse;
import com.w2m.backend.participant.entity.Participant;
import com.w2m.backend.participant.repository.ParticipantRepository;
import com.w2m.backend.vote.entity.Vote;
import com.w2m.backend.vote.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ParticipantService {


    private final ParticipantRepository participantRepository;
    private final MeetingRepository meetingRepository;
    private final UserRepository userRepository;
    private final VoteRepository voteRepository;
    private final com.w2m.backend.availability.repository.AvailabilityRepository availabilityRepository;
    private final CandidateReactionRepository candidateReactionRepository;

    @Transactional
    public ParticipantResponse joinMeeting(CreateParticipantRequest request, Long userId) {

        // 초대코드로 모임 조회
        Meeting meeting = meetingRepository.findByInviteCode(request.getInviteCode())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 초대코드입니다."));

        // 유저 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 이미 참여한 유저인지 확인
        boolean alreadyJoined = participantRepository
                .existsByMeetingIdAndUserId(meeting.getId(), user.getId());

        if (alreadyJoined) {
            throw new IllegalArgumentException("이미 참여한 모임입니다.");
        }

        // Participant Entity 생성
        Participant participant = new Participant(
                meeting,
                user,
                Participant.ParticipantRole.PARTICIPANT
        );

        // DB 저장
        Participant savedParticipant = participantRepository.save(participant);

        // Response DTO 반환
        return ParticipantResponse.from(savedParticipant, false);
    }

    @Transactional(readOnly = true)
    public List<ParticipantResponse> getParticipants(Long meetingId) {
        // 이 모임에서 누가 투표했는지 participantId 집합으로 미리 뽑아둠
        Set<Long> votedParticipantIds = voteRepository.findByMeetingId(meetingId)
                .stream()
                .map(vote -> vote.getParticipant().getId())
                .collect(Collectors.toSet());

        return participantRepository.findByMeetingId(meetingId)
                .stream()
                .map(p -> ParticipantResponse.from(p, votedParticipantIds.contains(p.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ParticipantResponse getMyParticipant(Long meetingId, Long userId) {
        Participant participant = participantRepository.findByMeetingIdAndUserId(meetingId, userId)
                .orElseThrow(() -> new IllegalArgumentException("참여 정보가 존재하지 않습니다."));
        boolean isVoted = voteRepository.findByMeetingIdAndParticipantId(meetingId, participant.getId()).isPresent();
        return ParticipantResponse.from(participant, isVoted);
    }
    @Transactional
    public void leaveMeeting(Long meetingId, Long userId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("모임이 존재하지 않습니다."));
        if (meeting.getHostUserId().equals(userId)) {
            throw new IllegalArgumentException("방장은 나가기할 수 없습니다. 모임 삭제를 사용해주세요.");
        }
        Participant participant = participantRepository.findByMeetingIdAndUserId(meetingId, userId)
                .orElseThrow(() -> new IllegalArgumentException("참여 정보가 존재하지 않습니다."));
        // votes/candidate_reactions는 participant 쪽에 cascade가 안 걸려있어서,
        // 그대로 두고 participant를 지우면 FK 위반이 남. 그래서 먼저 직접 지워줌
        voteRepository.deleteByParticipantId(participant.getId());
        candidateReactionRepository.deleteByParticipantId(participant.getId());

        // CascadeType.ALL 설정으로 인해 participant 삭제 시
        // 해당 참여자의 availabilities/locations가 모두 자동 삭제됩니다.
        participantRepository.delete(participant);
    }

}
