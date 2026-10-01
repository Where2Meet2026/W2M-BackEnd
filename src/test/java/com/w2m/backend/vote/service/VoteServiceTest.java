package com.w2m.backend.vote.service;

import com.w2m.backend.auth.entity.User;
import com.w2m.backend.auth.repository.UserRepository;
import com.w2m.backend.candidate.entity.PlaceCandidate;
import com.w2m.backend.candidate.repository.PlaceCandidateRepository;
import com.w2m.backend.meeting.entity.Meeting;
import com.w2m.backend.meeting.repository.MeetingRepository;
import com.w2m.backend.participant.entity.Participant;
import com.w2m.backend.participant.repository.ParticipantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class VoteServiceTest {

    @Autowired private VoteService voteService;
    @Autowired private UserRepository userRepository;
    @Autowired private MeetingRepository meetingRepository;
    @Autowired private ParticipantRepository participantRepository;
    @Autowired private PlaceCandidateRepository placeCandidateRepository;

    @Test
    void 전원투표하면자동으로확정된다() {
        User userA = userRepository.findById(14L).orElseThrow();
        User userB = userRepository.findById(15L).orElseThrow();

        Meeting meeting = meetingRepository.save(
                new Meeting(userA.getId(), "테스트 모임", "vote 테스트",
                        "TEST" + System.currentTimeMillis() % 100000, Meeting.PlaceCategory.CAFE));
        meeting.updateStatus(Meeting.MeetingStatus.VOTING);

        Participant participantA = participantRepository.save(
                new Participant(meeting, userA, Participant.ParticipantRole.HOST));
        Participant participantB = participantRepository.save(
                new Participant(meeting, userB, Participant.ParticipantRole.PARTICIPANT));

        PlaceCandidate candidate1 = placeCandidateRepository.save(PlaceCandidate.builder()
                .meeting(meeting).kakaoPlaceId("test1").placeName("테스트카페1")
                .address("서울").latitude(37.5).longitude(127.0)
                .type(PlaceCandidate.CandidateType.FASTEST)
                .avgDistanceMeters(100.0).maxDistanceMeters(100.0).build());
        PlaceCandidate candidate2 = placeCandidateRepository.save(PlaceCandidate.builder()
                .meeting(meeting).kakaoPlaceId("test2").placeName("테스트카페2")
                .address("서울").latitude(200.0).longitude(127.0)
                .type(PlaceCandidate.CandidateType.BALANCED)
                .avgDistanceMeters(200.0).maxDistanceMeters(200.0).build());

        // 1. A가 candidate1에 투표 → 아직 마감 아님
        boolean isClosedAfterA = voteService.castVote(meeting.getId(), candidate1.getId(), userA.getId());
        assertThat(isClosedAfterA).isFalse();

        // 2. B도 candidate1에 투표(마지막 사람) → 자동 마감
        boolean isClosedAfterB = voteService.castVote(meeting.getId(), candidate1.getId(), userB.getId());
        assertThat(isClosedAfterB).isTrue();

        // 3. 확정된 후보가 candidate1이 맞는지 확인
        Meeting confirmedMeeting = meetingRepository.findById(meeting.getId()).orElseThrow();
        assertThat(confirmedMeeting.getConfirmedCandidate().getId()).isEqualTo(candidate1.getId());
    }

    @Test
    void 두참여자가_동시에_마지막투표를해도_정확히한번만_확정된다() throws InterruptedException {
        User userA = userRepository.findById(14L).orElseThrow();
        User userB = userRepository.findById(15L).orElseThrow();

        Meeting meeting = meetingRepository.save(
                new Meeting(userA.getId(), "동시성 테스트 모임", "vote 동시성 테스트",
                        "CONC" + System.currentTimeMillis() % 100000, Meeting.PlaceCategory.CAFE));
        meeting.updateStatus(Meeting.MeetingStatus.VOTING);

        Participant participantA = participantRepository.save(
                new Participant(meeting, userA, Participant.ParticipantRole.HOST));
        Participant participantB = participantRepository.save(
                new Participant(meeting, userB, Participant.ParticipantRole.PARTICIPANT));

        PlaceCandidate candidate1 = placeCandidateRepository.save(PlaceCandidate.builder()
                .meeting(meeting).kakaoPlaceId("conc1").placeName("동시성카페1")
                .address("서울").latitude(37.5).longitude(127.0)
                .type(PlaceCandidate.CandidateType.FASTEST)
                .avgDistanceMeters(100.0).maxDistanceMeters(100.0).build());
        PlaceCandidate candidate2 = placeCandidateRepository.save(PlaceCandidate.builder()
                .meeting(meeting).kakaoPlaceId("conc2").placeName("동시성카페2")
                .address("서울").latitude(200.0).longitude(127.0)
                .type(PlaceCandidate.CandidateType.BALANCED)
                .avgDistanceMeters(200.0).maxDistanceMeters(200.0).build());

        // 두 참여자가 정확히 같은 순간에 투표를 보내도록, 신호(latch)가 떨어지기 전까지 대기시켜둠
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Boolean> results = Collections.synchronizedList(new ArrayList<>());

        Runnable voteAsA = () -> {
            try {
                startLatch.await();
                results.add(voteService.castVote(meeting.getId(), candidate1.getId(), userA.getId()));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
        Runnable voteAsB = () -> {
            try {
                startLatch.await();
                results.add(voteService.castVote(meeting.getId(), candidate1.getId(), userB.getId()));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        Thread threadA = new Thread(voteAsA);
        Thread threadB = new Thread(voteAsB);
        threadA.start();
        threadB.start();
        startLatch.countDown(); // 두 스레드를 동시에 출발시킴
        threadA.join();
        threadB.join();

        // 락이 제대로 걸렸다면, 두 번의 투표 중 정확히 한 번만 "마감됨(true)"이어야 함.
        // 락이 없었다면 둘 다 true(중복 확정 시도)이거나 둘 다 false(아무도 마감 못 봄)가 나올 수 있음.
        long closedCount = results.stream().filter(Boolean::booleanValue).count();
        assertThat(closedCount).isEqualTo(1);

        Meeting confirmedMeeting = meetingRepository.findById(meeting.getId()).orElseThrow();
        assertThat(confirmedMeeting.getStatus()).isEqualTo(Meeting.MeetingStatus.CONFIRMED);
    }
}
