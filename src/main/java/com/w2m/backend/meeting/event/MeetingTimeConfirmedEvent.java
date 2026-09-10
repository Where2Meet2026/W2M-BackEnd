package com.w2m.backend.meeting.event;

/**
 * 방장이 약속 시간을 확정했을 때 발행되는 도메인 이벤트.
 * confirmMeetingTime 트랜잭션 커밋 후 알림 리스너가 소비한다.
 */
public record MeetingTimeConfirmedEvent(Long meetingId) {
}
