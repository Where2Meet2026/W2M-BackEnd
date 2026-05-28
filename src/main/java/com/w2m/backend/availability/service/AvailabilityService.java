package com.w2m.backend.availability.service;

import com.w2m.backend.availability.dto.request.SaveAvailabilityRequest;
import com.w2m.backend.availability.entity.Availability;
import com.w2m.backend.availability.repository.AvailabilityRepository;
import com.w2m.backend.participant.entity.Participant;
import com.w2m.backend.participant.repository.ParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final AvailabilityRepository availabilityRepository;
    private final ParticipantRepository participantRepository;

    /**
     * 참여자의 가능 시간대를 일괄 저장합니다.
     * 기존에 저장된 시간대가 있다면 모두 삭제하고 새로운 목록으로 대체합니다.
     */
    @Transactional
    public void saveAvailabilities(Long participantId, SaveAvailabilityRequest request) {
        Participant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 참여자입니다."));

        // 1. 기존 데이터 삭제 (덮어쓰기 방식)
        availabilityRepository.deleteByParticipant(participant);

        // 2. 새로운 데이터 생성 및 저장
        List<Availability> availabilities = request.getTimeRanges().stream()
                .map(range -> Availability.builder()
                        .participant(participant)
                        .startDateTime(range.getStartDateTime())
                        .endDateTime(range.getEndDateTime())
                        .build())
                .collect(Collectors.toList());

        availabilityRepository.saveAll(availabilities);
    }

    /**
     * 특정 참여자의 모든 가능 시간대를 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<Availability> getAvailabilities(Long participantId) {
        Participant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 참여자입니다."));
        
        return availabilityRepository.findByParticipant(participant);
    }
}
