package com.w2m.backend.candidate.service;

import com.w2m.backend.candidate.entity.PlaceCandidate;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.w2m.backend.auth.entity.User;
import com.w2m.backend.auth.repository.UserRepository;
import com.w2m.backend.location.entity.ParticipantLocation;
import com.w2m.backend.location.repository.ParticipantLocationRepository;
import com.w2m.backend.meeting.entity.Meeting;
import com.w2m.backend.meeting.repository.MeetingRepository;
import com.w2m.backend.participant.entity.Participant;
import com.w2m.backend.participant.repository.ParticipantRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
@SpringBootTest
public class PlaceCandidateServiceTest {

    @Autowired private PlaceCandidateService placeCandidateService;
    @Autowired private UserRepository userRepository;
    @Autowired private MeetingRepository meetingRepository;
    @Autowired private ParticipantRepository participantRepository;
    @Autowired private ParticipantLocationRepository participantLocationRepository;


    @Test
    void 참여자가한명이그사람위치가그대로중간지점(){
        PlaceCandidateService.Coordinate centroid = placeCandidateService.calculateCentroid(20L);

        System.out.println("중간지점은 "+ centroid + "입니다!");
        assertThat(centroid.latitude()).isEqualTo(37.4979);
        assertThat(centroid.longitude()).isEqualTo(127.0276);
    }
    @Test
    void 후보3개를생성하고저장한다() {
        List<PlaceCandidate> candidates = placeCandidateService.generateCandidates(20L);

        candidates.forEach(c ->
                System.out.println(c.getType()+ "-" + c.getPlaceName() + "(" +
                        c.getAvgDistanceMeters() + "m)"));

        assertThat(candidates).hasSize(3);
    }

    @Disabled("실제 ODsay를 호출하므로 필요할 때만 주석을 풀어서 수동 실행")
    @Test
    void 후보를만들면_이동시간이_저장된다() {
        User userA = userRepository.findById(14L).orElseThrow();
        User userB = userRepository.findById(15L).orElseThrow();

        Meeting meeting = meetingRepository.save(
                new Meeting(userA.getId(), "[QA] ODsay 테스트", "이동시간 테스트",
                        "ODSY" + System.currentTimeMillis() % 100000, Meeting.PlaceCategory.CAFE));

        Participant a = participantRepository.save(
                new Participant(meeting, userA, Participant.ParticipantRole.HOST));
        Participant b = participantRepository.save(
                new Participant(meeting, userB, Participant.ParticipantRole.PARTICIPANT));

        // 서로 멀리 떨어진 두 위치: 신촌역, 건대입구역
        participantLocationRepository.save(ParticipantLocation.builder()
                .participant(a).address("신촌역").latitude(37.5551).longitude(126.9368).build());
        participantLocationRepository.save(ParticipantLocation.builder()
                .participant(b).address("건대입구역").latitude(37.5404).longitude(127.0692).build());

        List<PlaceCandidate> candidates = placeCandidateService.generateCandidates(meeting.getId());

        candidates.forEach(c -> System.out.println(
                c.getType() + " - " + c.getPlaceName()
                        + " / 평균 " + c.getAvgTravelMinutes() + "분, 최대 " + c.getMaxTravelMinutes() + "분"));

        assertThat(candidates).hasSize(3);
    }
}
