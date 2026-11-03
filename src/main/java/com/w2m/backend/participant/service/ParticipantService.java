package com.w2m.backend.participant.service;

import com.w2m.backend.auth.entity.User;
import com.w2m.backend.auth.repository.UserRepository;
import com.w2m.backend.candidate.repository.CandidateReactionRepository;
import com.w2m.backend.candidate.repository.PlaceCandidateRepository;
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
    private final PlaceCandidateRepository placeCandidateRepository;

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
        // 후보 장소가 하나라도 만들어졌으면 나가기를 막는다.
        // 1) 후보는 만들 때 '참여자 전원의 위치'로 계산된다. 그 뒤에 누가 나가면
        //    후보가 이미 없는 사람의 위치까지 반영된 채로 남는다.
        // 2) 투표는 후보가 있어야 할 수 있으므로, 이 검사로 '투표 중 나가기'도 같이 막힌다.
        //    (투표 확정은 누군가 투표할 때만 검사해서, 마지막 미투표자가 나가면 자동 확정이 안 되던 문제)
        // 3) 모임 상태(status)는 후보가 생겨도 COLLECTING_LOCATION 그대로라 상태로는 구분할 수 없다.
        //    그래서 상태 대신 후보가 DB에 실제로 있는지로 판단한다. 확정된 모임도 후보가 있으니 함께 막힌다.
        if (placeCandidateRepository.existsByMeetingId(meetingId)) {
            throw new IllegalStateException("장소 추천이 시작된 뒤에는 모임에서 나갈 수 없어요.");
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
