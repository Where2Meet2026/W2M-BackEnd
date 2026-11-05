package com.w2m.backend.location.service;

import com.w2m.backend.location.dto.request.SaveLocationRequest;
import com.w2m.backend.location.dto.response.LocationResponse;
import com.w2m.backend.location.entity.ParticipantLocation;
import com.w2m.backend.location.repository.ParticipantLocationRepository;
import com.w2m.backend.meeting.event.MeetingLocationsCompletedEvent;
import com.w2m.backend.meeting.repository.MeetingRepository;
import com.w2m.backend.participant.entity.Participant;
import com.w2m.backend.participant.repository.ParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final ParticipantLocationRepository participantLocationRepository;
    private final ParticipantRepository participantRepository;
    private final MeetingRepository meetingRepository;
    private final ApplicationEventPublisher eventPublisher;

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

        // 같은 모임의 위치 저장을 한 건씩 처리해서, 마지막 입력자를 정확히 한 번만 판정한다.
        // (참여자 행을 고치기 전에 잠가야 동시에 입력한 두 사람이 서로를 기다리는 교착이 생기지 않는다)
        Long meetingId = participant.getMeeting().getId();
        meetingRepository.findWithLockById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 모임입니다."));
        boolean wasLocationSelected = participant.isLocationSelected();

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

        // 이번 입력으로 전원의 위치가 모였으면 커밋 후 투표 안내 알림 발송 (NotificationEventListener).
        // 이미 입력했던 사람이 위치만 바꾸는 경우에는 다시 보내지 않는다.
        if (!wasLocationSelected && allLocationsSelected(meetingId)) {
            eventPublisher.publishEvent(new MeetingLocationsCompletedEvent(meetingId));
        }
    }

    private boolean allLocationsSelected(Long meetingId) {
        return participantRepository.findLatestByMeetingId(meetingId).stream()
                .allMatch(Participant::isLocationSelected);
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
