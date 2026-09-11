package com.w2m.backend.candidate.service;

import com.w2m.backend.candidate.client.KakaoLocalApiClient;
import com.w2m.backend.candidate.dto.response.CandidateResponse;
import com.w2m.backend.candidate.entity.CandidateReaction;
import com.w2m.backend.candidate.entity.PlaceCandidate;
import com.w2m.backend.candidate.repository.CandidateReactionRepository;
import com.w2m.backend.candidate.repository.PlaceCandidateRepository;
import com.w2m.backend.candidate.util.DistanceCalculator;
import com.w2m.backend.location.entity.ParticipantLocation;
import com.w2m.backend.location.repository.ParticipantLocationRepository;
import com.w2m.backend.meeting.entity.Meeting;
import com.w2m.backend.meeting.repository.MeetingRepository;
import com.w2m.backend.participant.entity.Participant;
import com.w2m.backend.participant.repository.ParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.w2m.backend.candidate.dto.response.KakaoLocalSearchResponse.KakaoPlaceDto;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PlaceCandidateService {

    private final ParticipantLocationRepository participantLocationRepository;
    private final PlaceCandidateRepository placeCandidateRepository;
    private final MeetingRepository meetingRepository;
    private final KakaoLocalApiClient kakaoLocalApiClient;
    private final CandidateReactionRepository candidateReactionRepository;

    private static final int SEARCH_RADIUS_METERS = 1500;
    private final ParticipantRepository participantRepository;

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

    /* 리뷰 없는 초기 단계용 설명 문장 — 우리가 계산한 실제 데이터로만 구성
    근데 문장력이 좀 아쉬워서 리뷰 도메인 완성 후 LLM API 붙여서
    API에서 받아온 실제 데이터들을 LLM에 넘겨 자연스러운 문장 구현 해달라고 할 예정 */
    private String buildDescription(PlaceCandidate.CandidateType type, CandidateScore score) {
        int distance = (int) Math.round(score.avgDistance());

        return switch (type) {
            case FASTEST -> String.format("참여자들과 평균 %dm로 가장 가까운 곳이에요!", distance);
            case BALANCED -> String.format("참여자들 간 이동 거리 차이가 적어, 다 같이 부담 없이 모일 수 있는 곳이에요. (평균 %dm)", distance);
            case OPTIMAL -> String.format("평균 %dm 거리의 무난한 곳이에요.", distance);
        };
    }
    public record Coordinate(double latitude, double longitude){}

    private record CandidateScore(KakaoPlaceDto place, double avgDistance, double maxDistance,
  double spread) {}

    public List<CandidateResponse> getCandidates(Long meetingId ,Long userId) {
        List<PlaceCandidate> candidates = placeCandidateRepository.findByMeetingId(meetingId);
        if (candidates.isEmpty()) { //후보 없으면 그 자리에서 생성
            candidates = generateCandidates(meetingId);
        }

        Participant participant = participantRepository.findByMeetingIdAndUserId(meetingId,userId)
                .orElseThrow(() -> new IllegalArgumentException("이 모임의 참여자가 아닙니다."));

        return candidates.stream()
                .map(candidate -> {
                    long likeCount = candidateReactionRepository.countByCandidateAndReactionType(
                            candidate, CandidateReaction.ReactionType.LIKE);
                    long dislikeCount = candidateReactionRepository.countByCandidateAndReactionType(
                            candidate, CandidateReaction.ReactionType.DISLIKE);
                    CandidateReaction.ReactionType myReaction = candidateReactionRepository.
                            findByCandidateAndParticipant(candidate, participant)
                            .map(CandidateReaction::getReactionType).
                            orElse(null);

                    return CandidateResponse.of(candidate, likeCount, dislikeCount, myReaction);
                })
                .toList();
    }
    @Transactional
    public void toggleReaction(Long meetingId, Long candidateId, Long userId,
   CandidateReaction.ReactionType requestedType) {
        Participant participant = participantRepository.findByMeetingIdAndUserId(meetingId, userId)
                .orElseThrow(() -> new IllegalArgumentException("이 모임의 참가자가 아닙니다."));

        PlaceCandidate candidate = placeCandidateRepository.findById(candidateId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 후보입니다."));

        Optional<CandidateReaction> existing =
                candidateReactionRepository.findByCandidateAndParticipant(candidate, participant);

        if(existing.isPresent()) {
            if (existing.get().getReactionType() == requestedType) {
                candidateReactionRepository.delete(existing.get()); // 같은 반응 다시 누르면 취소
            } else {
                existing.get().updateReactionType(requestedType); // 다른 반응으로 변경
            }
        }else {
            candidateReactionRepository.save(CandidateReaction.builder()
                    .candidate(candidate)
                    .participant(participant)
                    .reactionType(requestedType)
                    .build());
        }
    }
}


