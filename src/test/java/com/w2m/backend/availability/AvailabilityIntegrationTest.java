package com.w2m.backend.availability;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.w2m.backend.auth.entity.Provider;
import com.w2m.backend.auth.entity.User;
import com.w2m.backend.auth.repository.UserRepository;
import com.w2m.backend.availability.dto.request.SaveAvailabilityRequest;
import com.w2m.backend.availability.entity.Availability;
import com.w2m.backend.availability.repository.AvailabilityRepository;
import com.w2m.backend.meeting.entity.Meeting;
import com.w2m.backend.meeting.repository.MeetingRepository;
import com.w2m.backend.participant.entity.Participant;
import com.w2m.backend.participant.repository.ParticipantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class AvailabilityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private AvailabilityRepository availabilityRepository;

    @Autowired
    private UserRepository userRepository;

    private Meeting meeting;
    private Participant participant;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .email("test@test.com")
                .name("테스터")
                .provider(Provider.LOCAL)
                .build();
        userRepository.save(user);

        meeting = new Meeting(user.getId(), "테스트 모임", "설명", UUID.randomUUID().toString().substring(0, 6));
        meetingRepository.save(meeting);

        participant = Participant.builder()
                .meeting(meeting)
                .user(user)
                .nickname("테스터")
                .build();
        participantRepository.save(participant);
    }

    @Test
    @DisplayName("참여자 가능 시간 일괄 저장 및 조회 테스트")
    @WithMockUser
    void saveAndGetAvailabilitiesTest() throws Exception {
        // given
        LocalDateTime start1 = LocalDateTime.of(2026, 6, 1, 9, 0, 0);
        LocalDateTime end1 = LocalDateTime.of(2026, 6, 1, 12, 0, 0);
        LocalDateTime start2 = LocalDateTime.of(2026, 6, 2, 18, 0, 0);
        LocalDateTime end2 = LocalDateTime.of(2026, 6, 2, 21, 0, 0);

        String jsonRequest = String.format(
                "{\"timeRanges\": [{\"startDateTime\": \"2026-06-01T09:00:00\", \"endDateTime\": \"2026-06-01T12:00:00\"}, " +
                "{\"startDateTime\": \"2026-06-02T18:00:00\", \"endDateTime\": \"2026-06-02T21:00:00\"}]}"
        );

        // when & then: 저장 (POST)
        mockMvc.perform(post("/api/participants/{participantId}/availabilities", participant.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isOk());

        // when & then: 조회 (GET)
        mockMvc.perform(get("/api/participants/{participantId}/availabilities", participant.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].startDateTime").value("2026-06-01T09:00:00"))
                .andExpect(jsonPath("$[1].startDateTime").value("2026-06-02T18:00:00"));
    }

    @Test
    @DisplayName("참여자 가능 시간 수정(덮어쓰기) 테스트")
    @WithMockUser
    void updateAvailabilitiesTest() throws Exception {
        // given: 기존 데이터 저장
        Availability oldAvailability = Availability.builder()
                .participant(participant)
                .startDateTime(LocalDateTime.of(2026, 6, 1, 9, 0, 0))
                .endDateTime(LocalDateTime.of(2026, 6, 1, 10, 0, 0))
                .build();
        availabilityRepository.save(oldAvailability);

        // 새로운 데이터 (수정용)
        String jsonRequest = "{\"timeRanges\": [{\"startDateTime\": \"2026-06-05T10:00:00\", \"endDateTime\": \"2026-06-05T12:00:00\"}]}";

        // when: 수정 (PUT)
        mockMvc.perform(put("/api/participants/{participantId}/availabilities", participant.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isOk());

        // then: 기존 데이터는 삭제되고 새로운 데이터만 남아야 함
        List<Availability> availabilities = availabilityRepository.findByParticipant(participant);
        assertThat(availabilities).hasSize(1);
        assertThat(availabilities.get(0).getStartDateTime()).isEqualTo(LocalDateTime.of(2026, 6, 5, 10, 0, 0));
    }
}
