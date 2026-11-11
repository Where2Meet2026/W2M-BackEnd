package com.w2m.backend.rag.listener;

import com.w2m.backend.candidate.event.PlaceCandidateCreatedEvent;
import com.w2m.backend.candidate.repository.PlaceCandidateRepository;
import com.w2m.backend.rag.service.PlaceDescriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class CandidateDescriptionEventListener {
    private final PlaceCandidateRepository candidateRepository;
    private final PlaceDescriptionService descriptionService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCandidateCreated(PlaceCandidateCreatedEvent event) {
        Long candidateId = event.candidateId();
        try {
            candidateRepository.findById(candidateId)
                    .filter(candidate -> candidate.getMeetingDescription() == null)
                    .flatMap(descriptionService::generateForCandidate)
                    .ifPresent(description -> candidateRepository.saveMeetingDescription(candidateId, description));
        } catch (RuntimeException error) {
            log.warn("[description-ai] 후보 설명 생성 실패 candidateId={}", candidateId, error);
        }
    }
}
