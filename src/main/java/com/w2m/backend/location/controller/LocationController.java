package com.w2m.backend.location.controller;

import com.w2m.backend.auth.jwt.CustomUserDetails;
import com.w2m.backend.location.dto.request.SaveLocationRequest;
import com.w2m.backend.location.dto.response.LocationResponse;
import com.w2m.backend.location.service.LocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/participants/{participantId}/locations")
public class LocationController {

    private final LocationService locationService;

    @PostMapping
    public void saveLocation(
            @PathVariable Long participantId,
            @Valid @RequestBody SaveLocationRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        locationService.saveLocation(participantId, request, userDetails.getUser().getId());
    }

    @GetMapping
    public LocationResponse getLocation(
            @PathVariable Long participantId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return locationService.getLocation(participantId, userDetails.getUser().getId());
    }
}
