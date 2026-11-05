package com.w2m.backend.meeting.event;

/**
 * 전원 투표로 약속 장소가 최종 확정됐을 때 발행되는 도메인 이벤트.
 * castVote 트랜잭션 커밋 후 알림 리스너가 소비한다.
 */
public record MeetingPlaceConfirmedEvent(Long meetingId) {
}
