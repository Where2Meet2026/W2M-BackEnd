package com.w2m.backend.notification.controller;

import com.w2m.backend.auth.jwt.CustomUserDetails;
import com.w2m.backend.notification.dto.request.SubscribeRequest;
import com.w2m.backend.notification.dto.request.UnsubscribeRequest;
import com.w2m.backend.notification.service.NotificationSubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationSubscriptionService notificationSubscriptionService;

    @PostMapping("/subscribe")
    @ResponseStatus(HttpStatus.CREATED)
    public void subscribe(
            @Valid @RequestBody SubscribeRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        notificationSubscriptionService.subscribe(userDetails.getUser().getId(), request);
    }

    @DeleteMapping("/subscribe")
    public void unsubscribe(
            @Valid @RequestBody UnsubscribeRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        notificationSubscriptionService.unsubscribe(userDetails.getUser().getId(), request.getEndpoint());
    }
}
