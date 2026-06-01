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

                // 1시간 단위 슬롯으로 쪼개기
                while (current.isBefore(end)) {
                    slotMap.computeIfAbsent(current, k -> new ArrayList<>()).add(name);
                    current = current.plusHours(1);
                }
            }
        }

        List<AvailableSlotDto> recommendedSlots = slotMap.entrySet().stream()
                .map(entry -> {
                    LocalDateTime start = entry.getKey();
                    List<String> currentParticipants = entry.getValue();
                    
                    // 해당 참여자 그룹이 연속해서 가능한 시간 계산
                    LocalDateTime end = start.plusHours(1);
                    while (slotMap.containsKey(end)) {
                        List<String> nextParticipants = slotMap.get(end);
                        // 현재 슬롯의 참여자들이 다음 슬롯에도 모두 포함되어 있는지 확인
                        if (new HashSet<>(nextParticipants).containsAll(currentParticipants)) {
                            end = end.plusHours(1);
                        } else {
                            break;
                        }
                    }
                    
                    return AvailableSlotDto.builder()
                            .startDateTime(start)
                            .endDateTime(end)
                            .availableCount(currentParticipants.size())
                            .participantNames(currentParticipants)
                            .build();
                })
                // 정렬: 1. 참여자 수(역순) 2. 지속 시간(역순) 3. 시작 시간(정순)
                .sorted((a, b) -> {
                    if (b.getAvailableCount() != a.getAvailableCount()) {
                        return b.getAvailableCount() - a.getAvailableCount();
                    }
                    long durationA = java.time.Duration.between(a.getStartDateTime(), a.getEndDateTime()).toHours();
                    long durationB = java.time.Duration.between(b.getStartDateTime(), b.getEndDateTime()).toHours();
                    if (durationB != durationA) {
                        return Long.compare(durationB, durationA);
                    }
                    return a.getStartDateTime().compareTo(b.getStartDateTime());
                })
                .limit(3)
                .collect(Collectors.toList());

        return RecommendationResponseDto.builder()
                .meetingId(meetingId)
                .totalParticipants(participants.size())
                .recommendedSlots(recommendedSlots)
                .build();
    }
}
