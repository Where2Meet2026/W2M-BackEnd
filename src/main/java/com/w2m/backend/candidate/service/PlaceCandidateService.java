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
import com.w2m.backend.rag.service.DefaultDescriptionFormatter;
import com.w2m.backend.rag.service.PlaceDescriptionService;
import com.w2m.backend.vote.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.w2m.backend.candidate.dto.response.KakaoLocalSearchResponse.KakaoPlaceDto;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlaceCandidateService {

    private final ParticipantLocationRepository participantLocationRepository;
    private final PlaceCandidateRepository placeCandidateRepository;
    private final MeetingRepository meetingRepository;
    private final KakaoLocalApiClient kakaoLocalApiClient;
    private final CandidateReactionRepository candidateReactionRepository;
    private final VoteRepository voteRepository;
    private final PlaceDescriptionService placeDescriptionService;

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

        List<KakaoPlaceDto> places = searchCandidatePlaces(centroid, meeting.getCategory());

        List<CandidateScore> scores = places.stream()
                .map(place -> scorePlace(place, locations))
                .sorted(Comparator.comparingDouble(CandidateScore:: avgDistance))
                .toList();

        if (scores.size() < 3) {
            throw new IllegalStateException("출발 위치 주변에서 추천할 장소를 3곳 이상 찾지 못했어요.");
        }

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
    private List<KakaoPlaceDto> searchCandidatePlaces(Coordinate centroid, Meeting.PlaceCategory category) {
        Meeting.PlaceCategory effectiveCategory = (category != null) ? category : Meeting.PlaceCategory.ANY;

        List<String> categoryCodes = switch (effectiveCategory) {
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

    /* 후보에 저장하는 기본 설명 — 우리가 계산한 실제 거리로만 구성 (예: "평균 265m · 가장 빠른 동선").
    리뷰가 충분히 쌓인 장소는 조회할 때 rag 패키지의 AI 설명이 이 문구 대신 내려간다. */
    private String buildDescription(PlaceCandidate.CandidateType type, CandidateScore score) {
        return DefaultDescriptionFormatter.format(type, score.avgDistance());
    }
    public record Coordinate(double latitude, double longitude){}

    private record CandidateScore(KakaoPlaceDto place, double avgDistance, double maxDistance,
  double spread) {}

    /* 후보 최초 생성. 전원이 위치를 입력하기 전에 만들면 먼저 입력한 사람 위치만으로 후보가 굳어버리므로 막는다.
    전원이 동시에 화면에 들어오면 요청이 겹치므로, 모임 행을 잠그고 다시 확인해서 후보가 한 번만 만들어지게 한다. */
    private List<PlaceCandidate> generateCandidatesOnce(Long meetingId) {
        meetingRepository.findWithLockById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 모임입니다!"));

        // 일반 조회는 잠금을 기다리기 전의 스냅샷(REPEATABLE READ)을 보므로, 잠금 읽기로 최신 상태를 다시 확인한다.
        List<PlaceCandidate> candidates = placeCandidateRepository.findLatestByMeetingId(meetingId);
        if (!candidates.isEmpty()) {
            return candidates;
        }

        List<Participant> participants = participantRepository.findByMeetingId(meetingId);
        if (participants.isEmpty() || !participants.stream().allMatch(Participant::isLocationSelected)) {
            throw new IllegalStateException("모든 참여자가 출발 위치를 입력한 뒤에 후보를 확인할 수 있습니다.");
        }
        return generateCandidates(meetingId);
    }

    @Transactional
    public List<CandidateResponse> getCandidates(Long meetingId ,Long userId) {
        // 모임 참여자가 아니면 후보 생성(외부 API 호출, 모임 잠금)을 일으키지 못하도록 먼저 확인한다.
        Participant participant = participantRepository.findByMeetingIdAndUserId(meetingId,userId)
                .orElseThrow(() -> new IllegalArgumentException("이 모임의 참여자가 아닙니다."));

        List<PlaceCandidate> candidates = placeCandidateRepository.findByMeetingId(meetingId);
        if (candidates.isEmpty()) { //후보 없으면 그 자리에서 생성
            candidates = generateCandidatesOnce(meetingId);
        }

        // 리뷰 기반 AI 설명이 저장된 장소는 그 설명을, 아니면 후보에 저장된 기본 문구를 쓴다. (LLM 호출 없이 읽기만 함)
        Map<String, String> aiDescriptions = placeDescriptionService.findCached(
                candidates.stream().map(PlaceCandidate::getKakaoPlaceId).toList());

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

                    String description = aiDescriptions.getOrDefault(candidate.getKakaoPlaceId(), candidate.getDescription());
                    return CandidateResponse.of(candidate, likeCount, dislikeCount, myReaction, description);
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

        // 확정 이후에 싫어요 과반으로 확정된 후보가 삭제되면 모임이 참조하는 행이 사라져 오류가 나므로 막는다.
        if (candidate.getMeeting().getStatus() == Meeting.MeetingStatus.CONFIRMED) {
            throw new IllegalStateException("이미 투표가 마감되어 반응을 바꿀 수 없습니다.");
        }

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
        checkAndReplaceIfMajorityDisLiked(meetingId, candidate);
    }
    private static final double MAJORITY_THRESHOLD = 0.5;

    private void checkAndReplaceIfMajorityDisLiked(Long meetingId, PlaceCandidate candidate) {
        long totalParticipants = participantRepository.findByMeetingId(meetingId).size();
        long dislikeCount = candidateReactionRepository.countByCandidateAndReactionType(
                candidate, CandidateReaction.ReactionType.DISLIKE);

        if (dislikeCount <= totalParticipants * MAJORITY_THRESHOLD) {
            return; //아직 과반 아님 ,그냥 끝냄
        }
        replaceCandidate(meetingId, candidate);
    }
    private void replaceCandidate(Long meetingId, PlaceCandidate oldCandidate) {
        Meeting meeting = oldCandidate.getMeeting();
        List<ParticipantLocation> locations =
    participantLocationRepository.findAllByMeetingId(meetingId);
        Coordinate centroid = calculateCentroid(meetingId);

        Set<String> excludePlaceNames = placeCandidateRepository.findByMeetingId(meetingId).stream() // 중복 없는 값들의 모임
                .map(PlaceCandidate::getPlaceName)
                .collect(Collectors.toSet());

        CandidateScore replacement = searchCandidatePlaces(centroid, meeting.getCategory()).stream()
                .map(place -> scorePlace(place,locations))
                .filter(score -> !excludePlaceNames.contains(score.place().placeName())) //이미 후보로 있는 장소는 제외함
                .min(Comparator.comparingDouble(CandidateScore::avgDistance))
                .orElseThrow(()-> new IllegalStateException("교체할 후보를 찾을 수 없습니다"));

        /* 재교체 순서: 반응(deleteByCandidate)·투표(deleteByCandidateId) 먼저 지우고 → 후보 지우고(delete) → 새 후보 저장(save).
        순서가 중요함 — CandidateReaction, Vote가 둘 다 candidate_id로 이 후보를 참조하고 있어서,
        먼저 안 지우면 외래키 때문에 후보를 못 지움.*/
        candidateReactionRepository.deleteByCandidate(oldCandidate);
        voteRepository.deleteByCandidateId(oldCandidate.getId());
        placeCandidateRepository.delete(oldCandidate);
        placeCandidateRepository.save(toEntity(meeting, replacement, oldCandidate.getType()));
    }

}


