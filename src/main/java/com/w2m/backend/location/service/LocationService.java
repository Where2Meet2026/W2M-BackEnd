package com.w2m.backend.location.service;

import com.w2m.backend.location.dto.request.SaveLocationRequest;
import com.w2m.backend.location.dto.response.LocationResponse;
import com.w2m.backend.location.entity.ParticipantLocation;
import com.w2m.backend.location.repository.ParticipantLocationRepository;
import com.w2m.backend.participant.entity.Participant;
import com.w2m.backend.participant.repository.ParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final ParticipantLocationRepository participantLocationRepository;
    private final ParticipantRepository participantRepository;

    /**
     * 참여자의 출발 위치를 저장합니다.
     * availabilities와 동일하게 등록/수정 구분 없이 덮어쓰는 upsert 방식이며,
     * 저장 시 참여자의 위치 입력 완료 상태(isLocationSelected)를 함께 갱신합니다.
     */
    @Transactional
    public void saveLocation(Long participantId, SaveLocationRequest request, Long loginUserId) {
        Participant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 참여자입니다."));

        if (!participant.getUser().getId().equals(loginUserId)) {
            throw new IllegalArgumentException("본인의 위치만 등록할 수 있습니다.");
        }

        participantLocationRepository.findByParticipant(participant)
                .ifPresentOrElse(
                        location -> location.update(
                                request.getAddress(), request.getLatitude(), request.getLongitude()),
                        () -> participantLocationRepository.save(ParticipantLocation.builder()
                                .participant(participant)
                                .address(request.getAddress())
                                .latitude(request.getLatitude())
                                .longitude(request.getLongitude())
                                .build())
                );

        participant.updateLocationSelected(true);
        participantRepository.save(participant);
    }

    /**
     * 참여자가 저장한 출발 위치를 조회합니다.
     */
    @Transactional(readOnly = true)
    public LocationResponse getLocation(Long participantId, Long loginUserId) {
        Participant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 참여자입니다."));

        if (!participant.getUser().getId().equals(loginUserId)) {
            throw new IllegalArgumentException("본인의 위치만 조회할 수 있습니다.");
        }

        ParticipantLocation location = participantLocationRepository.findByParticipant(participant)
                .orElseThrow(() -> new IllegalArgumentException("저장된 위치가 없습니다."));

        return LocationResponse.from(location);
    }
}
