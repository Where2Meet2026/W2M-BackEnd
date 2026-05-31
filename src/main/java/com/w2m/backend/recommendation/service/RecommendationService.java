package com.w2m.backend.recommendation.service;

import com.w2m.backend.availability.entity.Availability;
import com.w2m.backend.availability.repository.AvailabilityRepository;
import com.w2m.backend.participant.entity.Participant;
import com.w2m.backend.participant.repository.ParticipantRepository;
import com.w2m.backend.recommendation.dto.AvailableSlotDto;
import com.w2m.backend.recommendation.dto.RecommendationResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final ParticipantRepository participantRepository;
    private final AvailabilityRepository availabilityRepository;

    @Transactional(readOnly = true)
    public RecommendationResponseDto getRecommendations(Long meetingId) {
        List<Participant> participants = participantRepository.findByMeetingId(meetingId);
        
        // Map<SlotStartTime, List<ParticipantName>>
        Map<LocalDateTime, List<String>> slotMap = new TreeMap<>();

        for (Participant participant : participants) {
            List<Availability> availabilities = availabilityRepository.findByParticipant(participant);
            String name = participant.getUser().getName();

            for (Availability availability : availabilities) {
                LocalDateTime current = availability.getStartDateTime();
                LocalDateTime end = availability.getEndDateTime();

                // 30분 단위 슬롯으로 쪼개기
                while (current.isBefore(end)) {
                    slotMap.computeIfAbsent(current, k -> new ArrayList<>()).add(name);
                    current = current.plusMinutes(30);
                }
            }
        }

        List<AvailableSlotDto> recommendedSlots = slotMap.entrySet().stream()
                .map(entry -> AvailableSlotDto.builder()
                        .startDateTime(entry.getKey())
                        .endDateTime(entry.getKey().plusMinutes(30))
                        .availableCount(entry.getValue().size())
                        .participantNames(entry.getValue())
                        .build())
                // 가장 많이 겹치는 시간순, 시간이 같다면 빠른 시간순
                .sorted(Comparator.comparing(AvailableSlotDto::getAvailableCount).reversed()
                        .thenComparing(AvailableSlotDto::getStartDateTime))
                .collect(Collectors.toList());

        return RecommendationResponseDto.builder()
                .meetingId(meetingId)
                .totalParticipants(participants.size())
                .recommendedSlots(recommendedSlots)
                .build();
    }
}
