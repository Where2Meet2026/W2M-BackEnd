package com.w2m.backend.availability.service;

import com.w2m.backend.availability.dto.request.SaveAvailabilityRequest;
import com.w2m.backend.availability.entity.Availability;
import com.w2m.backend.availability.repository.AvailabilityRepository;
import com.w2m.backend.meeting.entity.Meeting;
import com.w2m.backend.meeting.event.MeetingTimeInputCompletedEvent;
import com.w2m.backend.meeting.repository.MeetingRepository;
import com.w2m.backend.participant.entity.Participant;
import com.w2m.backend.participant.repository.ParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final AvailabilityRepository availabilityRepository;
    private final ParticipantRepository participantRepository;
    private final MeetingRepository meetingRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 참여자의 가능 시간대를 일괄 저장합니다.
     * 기존에 저장된 시간대가 있다면 모두 삭제하고 새로운 목록으로 대체합니다.
     */
    @Transactional
    public void saveAvailabilities(Long participantId, SaveAvailabilityRequest request) {
        Participant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new IllegalArgumentException("참여자를 찾을 수 없어요."));

        // 같은 모임의 시간 저장을 한 건씩 처리해서, 마지막 입력자를 놓치지 않고 판정한다.
        // (참여자 행을 고치기 전에 잠가야 동시에 입력한 두 사람이 서로를 기다리는 교착이 생기지 않는다)
        Long meetingId = participant.getMeeting().getId();
        Meeting meeting = meetingRepository.findWithLockById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 모임입니다."));
        boolean wasTimeSelected = participant.isTimeSelected();

        // 1. 기존 데이터 삭제 (덮어쓰기 방식)
        availabilityRepository.deleteByParticipant(participant);

        // 1.5 참여자의 시간 입력 완료 상태 업데이트
        participant.updateTimeSelected(true);
        participantRepository.save(participant);

        // 2. 새로운 데이터 생성 및 저장
        List<Availability> availabilities = request.getTimeRanges().stream()
                .map(range -> Availability.builder()
                        .participant(participant)
                        .startDateTime(range.getStartDateTime())
                        .endDateTime(range.getEndDateTime())
                        .build())
                .collect(Collectors.toList());

        availabilityRepository.saveAll(availabilities);

        // 이번 입력으로 방장을 뺀 전원의 시간이 모였으면 커밋 후 방장에게 확정 요청 알림 발송 (NotificationEventListener).
        // 방장 본인의 입력, 이미 입력한 사람의 수정, 시간이 확정된 뒤의 입력에는 보내지 않는다.
        if (participant.getRole() != Participant.ParticipantRole.HOST
                && !wasTimeSelected
                && meeting.getConfirmedStartDateTime() == null
                && allGuestsTimeSelected(meetingId)) {
            eventPublisher.publishEvent(new MeetingTimeInputCompletedEvent(meetingId));
        }
    }

    private boolean allGuestsTimeSelected(Long meetingId) {
        return participantRepository.findLatestByMeetingId(meetingId).stream()
                .filter(p -> p.getRole() != Participant.ParticipantRole.HOST)
                .allMatch(Participant::isTimeSelected);
    }

    /**
     * 특정 참여자의 모든 가능 시간대를 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<Availability> getAvailabilities(Long participantId) {
        Participant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new IllegalArgumentException("참여자를 찾을 수 없어요."));
        
        return availabilityRepository.findByParticipant(participant);
    }
}
