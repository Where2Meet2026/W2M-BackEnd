package com.w2m.backend.candidate.service;

import com.w2m.backend.location.entity.ParticipantLocation;
import com.w2m.backend.location.repository.ParticipantLocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlaceCandidateService {

    private final ParticipantLocationRepository participantLocationRepository;

    // 참여자 전원 위치의 평균 좌표 = 카카오 장소 검색의 중심점으로 사용
    public Coordinate calculateCentroid(Long meetingId){
        List<ParticipantLocation> locations =
    participantLocationRepository.findAllByMeetingId(meetingId);

        double avgLat = locations.stream()
                .mapToDouble(ParticipantLocation::getLatitude)
                .average()
                .orElseThrow(() -> new IllegalStateException("위치 데이터가 없습니다"));

        double avgLng = locations.stream()
                .mapToDouble(ParticipantLocation::getLongitude)
                .average()
                .orElseThrow(() -> new IllegalStateException("위치 데이터가 없습니다"));

        return new Coordinate(avgLat, avgLng);
    }

    public record Coordinate(double latitude, double longitude){}
}
