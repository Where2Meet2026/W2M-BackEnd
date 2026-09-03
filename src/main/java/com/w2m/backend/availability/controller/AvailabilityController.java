package com.w2m.backend.availability.controller;

import com.w2m.backend.availability.dto.request.SaveAvailabilityRequest;
import com.w2m.backend.availability.entity.Availability;
import com.w2m.backend.availability.service.AvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.w2m.backend.availability.dto.response.AvailabilityResponse;
import jakarta.validation.Valid;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/participants/{participantId}/availabilities")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    @PostMapping
    public void saveAvailabilities(
            @PathVariable Long participantId,
            @Valid @RequestBody SaveAvailabilityRequest request) {
        availabilityService.saveAvailabilities(participantId, request);
    }

    @GetMapping
    public List<AvailabilityResponse> getAvailabilities(@PathVariable Long participantId) {
        return availabilityService.getAvailabilities(participantId).stream()
                .map(AvailabilityResponse::from)
                .collect(Collectors.toList());
    }
}
