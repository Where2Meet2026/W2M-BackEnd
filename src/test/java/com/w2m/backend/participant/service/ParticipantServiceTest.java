package com.w2m.backend.participant.service;

import com.w2m.backend.auth.entity.User;
import com.w2m.backend.auth.repository.UserRepository;
import com.w2m.backend.candidate.entity.PlaceCandidate;
import com.w2m.backend.candidate.repository.PlaceCandidateRepository;
import com.w2m.backend.meeting.dto.response.MeetingResponse;
import com.w2m.backend.meeting.entity.Meeting;
import com.w2m.backend.meeting.repository.MeetingRepository;
import com.w2m.backend.meeting.service.MeetingService;
import com.w2m.backend.participant.entity.Participant;
import com.w2m.backend.participant.repository.ParticipantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
public class ParticipantServiceTest {

    @Autowired private ParticipantService participantService;
    @Autowired private MeetingService meetingService;
    @Autowired private UserRepository userRepository;
    @Autowired private MeetingRepository meetingRepository;
    @Autowired private ParticipantRepository participantRepository;
    @Autowired private PlaceCandidateRepository placeCandidateRepository;

    // 방장 A와 참여자 B가 있는 모임을 만든다
    private Meeting createMeetingWithParticipant(User host, User member, String codePrefix) {
        Meeting meeting = meetingRepository.save(
                new Meeting(host.getId(), "나가기 테스트", "나가기 제한 테스트",
                        codePrefix + System.currentTimeMillis() % 100000, Meeting.PlaceCategory.CAFE));
        participantRepository.save(new Participant(meeting, host, Participant.ParticipantRole.HOST));
        participantRepository.save(new Participant(meeting, member, Participant.ParticipantRole.PARTICIPANT));
        return meeting;
    }

    // 모임에 후보 하나를 만들어 둔다 (= 장소 추천이 시작된 상태)
    private void addCandidate(Meeting meeting) {
        placeCandidateRepository.save(PlaceCandidate.builder()
                .meeting(meeting).kakaoPlaceId("leave1").placeName("나가기카페")
                .address("서울").latitude(37.5).longitude(127.0)
                .type(PlaceCandidate.CandidateType.FASTEST)
                .avgDistanceMeters(100.0).maxDistanceMeters(100.0).build());
    }

    // 내 방 목록에서 이 모임의 응답만 골라낸다
    private MeetingResponse findInMyMeetings(Long userId, Long meetingId) {
        return meetingService.getMyMeetings(userId).stream()
                .filter(m -> m.getMeetingId().equals(meetingId))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void 후보가_만들어진_뒤에는_참여자가_나갈_수_없다() {
        User userA = userRepository.findById(14L).orElseThrow();
        User userB = userRepository.findById(15L).orElseThrow();
        Meeting meeting = createMeetingWithParticipant(userA, userB, "LVNO");
        addCandidate(meeting);

        assertThatThrownBy(() -> participantService.leaveMeeting(meeting.getId(), userB.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("장소 추천이 시작된 뒤에는 모임에서 나갈 수 없어요.");

        // 나가기가 막혔으니 참여 정보는 그대로 남아 있어야 한다
        assertThat(participantRepository.findByMeetingIdAndUserId(meeting.getId(), userB.getId())).isPresent();
    }

    @Test
    void 후보가_없으면_참여자는_나갈_수_있다() {
        User userA = userRepository.findById(14L).orElseThrow();
        User userB = userRepository.findById(15L).orElseThrow();
        Meeting meeting = createMeetingWithParticipant(userA, userB, "LVOK");

        participantService.leaveMeeting(meeting.getId(), userB.getId());

        assertThat(participantRepository.findByMeetingIdAndUserId(meeting.getId(), userB.getId())).isEmpty();
    }

    @Test
    void 내방목록의_canLeave는_후보가_생기면_false가_된다() {
        User userA = userRepository.findById(14L).orElseThrow();
        User userB = userRepository.findById(15L).orElseThrow();
        Meeting meeting = createMeetingWithParticipant(userA, userB, "LVCL");

        // 후보가 없을 때: 참여자는 나갈 수 있다
        assertThat(findInMyMeetings(userB.getId(), meeting.getId()).isCanLeave()).isTrue();

        addCandidate(meeting);

        // 후보가 생긴 뒤: 나가기 버튼을 숨기도록 false
        assertThat(findInMyMeetings(userB.getId(), meeting.getId()).isCanLeave()).isFalse();
    }
}
