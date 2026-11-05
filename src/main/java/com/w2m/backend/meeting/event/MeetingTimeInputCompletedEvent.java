package com.w2m.backend.meeting.event;

/**
 * 방장을 제외한 참여자 전원이 가능 시간을 입력했을 때 발행되는 도메인 이벤트.
 * saveAvailabilities 트랜잭션 커밋 후 알림 리스너가 방장에게 시간 확정을 요청한다.
 */
public record MeetingTimeInputCompletedEvent(Long meetingId) {
}
