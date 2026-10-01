package com.w2m.backend.vote.service;

import com.w2m.backend.candidate.entity.CandidateReaction;
import com.w2m.backend.candidate.entity.PlaceCandidate;
import com.w2m.backend.candidate.repository.CandidateReactionRepository;
import com.w2m.backend.candidate.repository.PlaceCandidateRepository;
import com.w2m.backend.meeting.entity.Meeting;
import com.w2m.backend.meeting.repository.MeetingRepository;
import com.w2m.backend.participant.entity.Participant;
import com.w2m.backend.participant.repository.ParticipantRepository;
import com.w2m.backend.vote.dto.response.VoteResultItem;
import com.w2m.backend.vote.dto.response.VoteStatusResponse;
import com.w2m.backend.vote.entity.Vote;
import com.w2m.backend.vote.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VoteService {
    private final MeetingRepository meetingRepository;
    private final ParticipantRepository participantRepository;
    private final PlaceCandidateRepository placeCandidateRepository;
    private final VoteRepository voteRepository;
    private final CandidateReactionRepository candidateReactionRepository;

    // 후보 하나에 최종 투표(또는 변경). 반환값은 "이 요청으로 투표가 마감·확정됐는지" 여부
    @Transactional
    public boolean castVote(Long meetingId, Long candidateId, Long userId) {
        // 1. 이 모임 행을 잠그고 조회 — 이 트랜잭션이 끝날 때까지 같은 모임에 대한 다른 투표 요청은 여기서 대기
        //    (마지막 두 명이 동시에 투표해도 한 명씩 순서대로 처리되게 하려고 거는 락)
        Meeting meeting = meetingRepository.findWithLockById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지않는 모임입니다."));

        // 이미 확정(마감)된 모임이면 더 이상 투표를 받지 않음
        if (meeting.getStatus() == Meeting.MeetingStatus.CONFIRMED) {
            throw new IllegalStateException("이미 투표가 마감되었습니다.");
        }

        // 2. 요청한 유저가 이 모임의 참여자가 맞는지 확인
        Participant participant = participantRepository.findByMeetingIdAndUserId(meetingId, userId)
                .orElseThrow(() -> new IllegalArgumentException("이 모임의 참가자가 아닙니다."));

        // 3. 투표하려는 후보가 실제로 존재하는 후보인지 확인
        PlaceCandidate candidate = placeCandidateRepository.findById(candidateId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 후보입니다."));

        // 4. 이 참여자가 이 모임에서 이미 투표한 적 있으면 후보만 바꾸고(변경), 처음이면 새로 저장
        Optional<Vote> existingVote = voteRepository.findByMeetingIdAndParticipantId(meetingId, participant.getId());
        if(existingVote.isPresent()) {
            existingVote.get().changeCandidate(candidate); // 더티 체킹으로 자동 UPDATE — save() 필요 없음
        }
        else {
            Vote vote = Vote.builder()
                    .meeting(meeting)
                    .candidate(candidate)
                    .participant(participant)
                    .build();
            voteRepository.save(vote);
        }
        // 이 모임 참여자 총원과, 지금까지 투표한 인원(방금 저장/변경한 내 표 포함)을 비교
        long totalParticipants = participantRepository.findByMeetingId(meetingId).size();
        long votedCount = voteRepository.countByMeetingId(meetingId);

        // 아직 전원 투표 완료가 아니면 여기서 끝 — 집계/확정은 마지막 사람 투표 때만 함
        if(votedCount < totalParticipants) {
            return false;
        }

        // 여기부터는 "방금 내가 마지막 투표자"라는 뜻 → 득표 집계해서 1등(winner) 확정
        List<PlaceCandidate> candidates = placeCandidateRepository.findByMeetingId(meetingId);

        // 일단 첫 번째 후보를 임시 1등으로 놓고 시작 (그 후보의 득표수/좋아요수도 같이 기억해둠)
        PlaceCandidate winner = candidates.get(0);
        long winnerVoteCount = voteRepository.findByCandidateId(winner.getId()).size();
        long winnerLikeCount = candidateReactionRepository.countByCandidateAndReactionType(winner,
                CandidateReaction.ReactionType.LIKE);

        // 나머지 후보들을 한 명씩 현재 1등(winner)과 비교
        for (PlaceCandidate current: candidates ) {
            if (current == winner) {
                continue; // 지금 1등인 후보 자기 자신과는 비교할 필요 없음
            }

            long currentVoteCount = voteRepository.findByCandidateId(current.getId()).size();
            long currentLikeCount = candidateReactionRepository.countByCandidateAndReactionType(current,
                    CandidateReaction.ReactionType.LIKE);

            boolean currentIsBetter = false;

            // 1순위: 득표수가 더 많으면 무조건 승
            if (currentVoteCount > winnerVoteCount) {
                currentIsBetter = true;
            } else if(currentVoteCount == winnerVoteCount) {
                // 득표수가 같으면 → 2순위: 좋아요 수가 더 많은 쪽 승
                if(currentLikeCount > winnerLikeCount) {
                    currentIsBetter = true;
                }
                else if(currentLikeCount == winnerLikeCount) {
                    // 좋아요 수까지 같으면 → 3순위: 후보 타입 순서(빠른→균형→최적)가 빠른 쪽 승
                    if(current.getType().ordinal() < winner.getType().ordinal()) {
                        currentIsBetter = true;
                    }
                }

            }
            // (득표수가 더 적으면 이 세 블록 다 안 타서 currentIsBetter는 false로 남음 → 1등 유지)

            if (currentIsBetter) {
                // 1등이 바뀌었으니, 다음 후보와 비교할 기준값도 같이 새 1등 걸로 갱신
                winner = current;
                winnerVoteCount = currentVoteCount;
                winnerLikeCount = currentLikeCount;
            }
        }

        // 모든 후보를 다 비교해서 최종 winner가 정해진 뒤에, 이 모임을 그 후보로 확정
        meeting.confirmVote(winner);
        return true;
    }
    // 투표 현황 조회 — 데이터를 바꾸지 않는 읽기 전용이라 락(findWithLockById)은 안 쓰고
    // readOnly = true만 붙임 (POST가 락 잡고 있는 동안 이 조회까지 같이 멈추면 안 되니까)
    @Transactional(readOnly = true)
    public VoteStatusResponse getVoteStatus(Long meetingId, Long userId) {

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 모임입니다."));

        // 전체 참여자 수 대비 지금까지 투표한 인원, 그리고 마감(확정) 여부
        long totalParticipants = participantRepository.findByMeetingId(meetingId).size();
        long votedCount = voteRepository.countByMeetingId(meetingId);
        boolean isClosed = meeting.getStatus() == Meeting.MeetingStatus.CONFIRMED;

        // 요청한 유저 본인이 이 모임 참여자가 맞는지 확인 (아니면 애초에 이 정보를 볼 자격이 없음)
        Participant participant = participantRepository.findByMeetingIdAndUserId(meetingId, userId)
                .orElseThrow(() -> new IllegalArgumentException(("이 모임의 참가자가 아닙니다.")));

        // 본인이 이 모임에서 누구에게 투표했는지 — 아직 투표 안 했으면 null
        Optional<Vote> myVote = voteRepository.findByMeetingIdAndParticipantId(meetingId,
                participant.getId());
            Long myVoteCandidateId = null;
            if(myVote.isPresent()){
                myVoteCandidateId = myVote.get().getCandidate().getId();
            }
            // 아직 확정 전이면 null, 확정됐으면 그 후보의 id
            Long confirmedCandidateId = null;
            if (meeting.getConfirmedCandidate() != null) {
                confirmedCandidateId = meeting.getConfirmedCandidate().getId();
            }

            // 후보별 득표수 목록 — 프론트에서 투표 결과/순위 보여줄 때 씀
            List<PlaceCandidate> candidates =
                    placeCandidateRepository.findByMeetingId(meetingId);
            List<VoteResultItem> results = new ArrayList<>();
            for (PlaceCandidate candidate: candidates) {
                long voteCount = voteRepository.findByCandidateId(candidate.getId()).size();
                results.add(VoteResultItem.of(candidate.getId(), voteCount));
            }
            return VoteStatusResponse.of(totalParticipants,votedCount,isClosed,myVoteCandidateId,confirmedCandidateId,results);

    }


}
