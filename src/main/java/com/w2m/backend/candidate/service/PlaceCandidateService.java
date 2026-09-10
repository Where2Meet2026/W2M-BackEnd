package com.w2m.backend.candidate.service;

import com.w2m.backend.candidate.client.KakaoLocalApiClient;
import com.w2m.backend.candidate.dto.response.KakaoLocalSearchResponse;
import com.w2m.backend.candidate.entity.PlaceCandidate;
import com.w2m.backend.candidate.repository.PlaceCandidateRepository;
import com.w2m.backend.candidate.util.DistanceCalculator;
import com.w2m.backend.location.entity.ParticipantLocation;
import com.w2m.backend.location.repository.ParticipantLocationRepository;
import com.w2m.backend.meeting.entity.Meeting;
import com.w2m.backend.meeting.repository.MeetingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.w2m.backend.candidate.dto.response.KakaoLocalSearchResponse.KakaoPlaceDto;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlaceCandidateService {

    private final ParticipantLocationRepository participantLocationRepository;
    private final PlaceCandidateRepository placeCandidateRepository;
    private final MeetingRepository meetingRepository;
    private final KakaoLocalApiClient kakaoLocalApiClient;

    private static final int SEARCH_RADIUS_METERS = 1500;

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

    public List<PlaceCandidate> generateCandidates(Long meetingId){
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 모임입니다!"));
        List<ParticipantLocation> locations =
    participantLocationRepository.findAllByMeetingId(meetingId);
        Coordinate centroid = calculateCentroid(meetingId);

        List<KakaoPlaceDto> places = searchCandidatePlaces(centroid, meeting.getPurpose());

        List<CandidateScore> scores = places.stream()
                .map(place -> scorePlace(place, locations))
                .sorted(Comparator.comparingDouble(CandidateScore:: avgDistance))
                .toList();

        CandidateScore fastest = scores.get(0);
        CandidateScore optimal = scores.get(1); // review 도메인 아직 없어서 임시: 평균거리 2등(9.10)

        CandidateScore balanced = scores.stream()
                .filter(s -> !s.place().placeName().equals(fastest.place().placeName())
                && !s.place().placeName().equals(optimal.place().placeName()))
                .min(Comparator.comparingDouble(CandidateScore::spread))
                .orElse(scores.get(2));

            List<PlaceCandidate> candidates = List.of(
                    toEntity(meeting, fastest, PlaceCandidate.CandidateType.FASTEST),
                    toEntity(meeting, balanced , PlaceCandidate.CandidateType.BALANCED),
                    toEntity(meeting, optimal , PlaceCandidate.CandidateType.OPTIMAL)
            );
            return placeCandidateRepository.saveAll(candidates);
    }
    // 후보 장소 하나 당, 참여자 전원과의 거리를 계산해서 평균 / 최대 / 편차를 구함
    private CandidateScore scorePlace(KakaoPlaceDto place,List<ParticipantLocation> locations) {
        double placeLat = Double.parseDouble(place.latitude());
        double placeLng = Double.parseDouble(place.longitude());

        List<Double> distances = locations.stream()
                .map(loc -> DistanceCalculator.calculateDistance(
                        loc.getLatitude(), loc.getLongitude() , placeLat, placeLng))
                .toList();

        double avg = distances.stream().mapToDouble(d -> d).average().orElse(0);
        double max = Collections.max(distances);
        double min = Collections.min(distances);

        return new CandidateScore(place, avg, max, max - min);
    }
    private List<KakaoPlaceDto> searchCandidatePlaces(Coordinate centroid, Meeting.MeetingPurpose purpose) {
        Meeting.MeetingPurpose effectivePurpose = (purpose != null) ? purpose : Meeting.MeetingPurpose.ANY;

        List<String> categoryCodes = switch (effectivePurpose) {
            case MEAL -> List.of("FD6");
            case CAFE -> List.of("CE7");
            case ANY -> List.of("FD6", "CE7");
        };

        return categoryCodes.stream()
                .flatMap(code -> kakaoLocalApiClient.searchByCategory(
                                centroid.latitude(), centroid.longitude(), SEARCH_RADIUS_METERS, code)
                        .documents().stream())
                .toList();
    }
    private PlaceCandidate toEntity(Meeting meeting, CandidateScore score,
                                    PlaceCandidate.CandidateType type) {
        return PlaceCandidate.builder()
                .meeting(meeting)
                .kakaoPlaceId(score.place().placeId())
                .placeName(score.place().placeName())
                .address(score.place().addressName())
                .latitude(Double.parseDouble(score.place().latitude()))
                .longitude(Double.parseDouble(score.place().longitude()))
                .type(type)
                .avgDistanceMeters(score.avgDistance())
                .maxDistanceMeters(score.maxDistance())
                .description(buildDescription(type, score))
                .build();
    }

    // 리뷰 없는 초기 단계용 설명 문장 — 우리가 계산한 실제 데이터로만 구성
    private String buildDescription(PlaceCandidate.CandidateType type, CandidateScore score) {
        String reason = switch (type) {
            case FASTEST -> "참여자들과 평균적으로 가장 가까운 곳이에요";
            case BALANCED -> "참여자들 간 이동 거리가 가장 고르게 나뉘는 곳이에요";
            case OPTIMAL -> "그다음으로 무난하게 가까운 곳이에요";
        };
        return String.format("%s (%s, 평균 %.0fm)", reason, score.place().categoryName(), score.avgDistance());
    }
    public record Coordinate(double latitude, double longitude){}

    private record CandidateScore(KakaoPlaceDto place, double avgDistance, double maxDistance,
  double spread) {}
}


