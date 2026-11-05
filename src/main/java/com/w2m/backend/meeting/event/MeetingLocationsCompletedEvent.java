package com.w2m.backend.meeting.event;

/**
 * 마지막 참여자가 출발 위치를 입력해서 모임 전원의 위치가 모였을 때 발행되는 도메인 이벤트.
 * saveLocation 트랜잭션 커밋 후 알림 리스너가 투표 안내 알림을 보낸다.
 */
public record MeetingLocationsCompletedEvent(Long meetingId) {
}
