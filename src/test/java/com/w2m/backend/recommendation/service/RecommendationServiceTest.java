package com.w2m.backend.recommendation.service;

import com.w2m.backend.auth.entity.Provider;
import com.w2m.backend.auth.entity.User;
import com.w2m.backend.availability.entity.Availability;
import com.w2m.backend.availability.repository.AvailabilityRepository;
import com.w2m.backend.participant.entity.Participant;
import com.w2m.backend.participant.repository.ParticipantRepository;
import com.w2m.backend.recommendation.dto.AvailableSlotDto;
import com.w2m.backend.recommendation.dto.RecommendationResponseDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock private ParticipantRepository participantRepository;
    @Mock private AvailabilityRepository availabilityRepository;

    @InjectMocks
    private RecommendationService recommendationService;

    private static final Long MEETING_ID = 1L;

    private Participant participant(String name) {
        User user = User.builder().email(name + "@w2m.com").name(name).provider(Provider.LOCAL).build();
        return new Participant(null, user, Participant.ParticipantRole.PARTICIPANT);
    }

    private LocalDateTime at(int day, int hour) {
        return LocalDateTime.of(2026, 11, day, hour, 0);
    }

    private void available(Participant p, LocalDateTime start, LocalDateTime end) {
        when(availabilityRepository.findByParticipant(p)).thenReturn(List.of(
                Availability.builder().participant(p).startDateTime(start).endDateTime(end).build()));
    }

    private List<AvailableSlotDto> recommend(Participant... participants) {
        when(participantRepository.findByMeetingId(MEETING_ID)).thenReturn(List.of(participants));
        RecommendationResponseDto result = recommendationService.getRecommendations(MEETING_ID);
        return result.getRecommendedSlots();
    }

    @Test
    void 두명이_같은_3시간이_가능하면_한_구간만_추천한다() {
        Participant a = participant("A");
        Participant b = participant("B");
        available(a, at(2, 10), at(2, 13));
        available(b, at(2, 10), at(2, 13));

        List<AvailableSlotDto> slots = recommend(a, b);

        // 10~13시 하나만 나오고, 11~13시/12~13시 같은 뒷부분 조각은 나오지 않는다.
        assertThat(slots).hasSize(1);
        assertThat(slots.get(0).getStartDateTime()).isEqualTo(at(2, 10));
        assertThat(slots.get(0).getEndDateTime()).isEqualTo(at(2, 13));
        assertThat(slots.get(0).getAvailableCount()).isEqualTo(2);
    }

    @Test
    void 한명이_먼저_가능하면_둘이_겹치는_구간과_혼자_가능한_구간을_따로_추천한다() {
        Participant a = participant("A");
        Participant b = participant("B");
        available(a, at(2, 10), at(2, 13));
        available(b, at(2, 11), at(2, 13));

        List<AvailableSlotDto> slots = recommend(a, b);

        assertThat(slots).hasSize(2);
        // 2명이 겹치는 11~13시가 먼저, 그다음 A 혼자 가능한 10~13시. 12~13시 조각은 없다.
        assertThat(slots.get(0).getStartDateTime()).isEqualTo(at(2, 11));
        assertThat(slots.get(0).getEndDateTime()).isEqualTo(at(2, 13));
        assertThat(slots.get(0).getAvailableCount()).isEqualTo(2);
        assertThat(slots.get(1).getStartDateTime()).isEqualTo(at(2, 10));
        assertThat(slots.get(1).getAvailableCount()).isEqualTo(1);
    }

    @Test
    void 떨어진_두_구간은_둘_다_추천에_남는다() {
        Participant a = participant("A");
        Participant b = participant("B");
        when(availabilityRepository.findByParticipant(a)).thenReturn(List.of(
                Availability.builder().participant(a).startDateTime(at(2, 10)).endDateTime(at(2, 12)).build(),
                Availability.builder().participant(a).startDateTime(at(2, 15)).endDateTime(at(2, 17)).build()));
        when(availabilityRepository.findByParticipant(b)).thenReturn(List.of(
                Availability.builder().participant(b).startDateTime(at(2, 10)).endDateTime(at(2, 12)).build(),
                Availability.builder().participant(b).startDateTime(at(2, 15)).endDateTime(at(2, 17)).build()));

        List<AvailableSlotDto> slots = recommend(a, b);

        assertThat(slots).hasSize(2);
        assertThat(slots.get(0).getStartDateTime()).isEqualTo(at(2, 10));
        assertThat(slots.get(0).getEndDateTime()).isEqualTo(at(2, 12));
        assertThat(slots.get(1).getStartDateTime()).isEqualTo(at(2, 15));
        assertThat(slots.get(1).getEndDateTime()).isEqualTo(at(2, 17));
    }

    @Test
    void 앞_구간이_더_일찍_끝나면_뒤_구간은_조각이_아니므로_남긴다() {
        Participant a = participant("A");
        Participant b = participant("B");
        Participant c = participant("C");
        available(a, at(2, 10), at(2, 13));
        available(b, at(2, 10), at(2, 11));
        available(c, at(2, 10), at(2, 11));

        List<AvailableSlotDto> slots = recommend(a, b, c);

        // 10~11시(3명)와 A 혼자 11~13시. 12~13시는 11~13시의 조각이라 빠진다.
        assertThat(slots).hasSize(2);
        assertThat(slots.get(0).getStartDateTime()).isEqualTo(at(2, 10));
        assertThat(slots.get(0).getAvailableCount()).isEqualTo(3);
        assertThat(slots.get(1).getStartDateTime()).isEqualTo(at(2, 11));
        assertThat(slots.get(1).getEndDateTime()).isEqualTo(at(2, 13));
        assertThat(slots.get(1).getAvailableCount()).isEqualTo(1);
    }

    @Test
    void 입력된_가능시간이_없으면_빈_목록을_반환한다() {
        Participant a = participant("A");
        when(availabilityRepository.findByParticipant(a)).thenReturn(List.of());

        assertThat(recommend(a)).isEmpty();
    }
}
