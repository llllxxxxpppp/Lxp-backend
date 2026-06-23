package com.lcs.lxp.subscription.presentation;

import com.lcs.lxp.subscription.application.dto.response.SubscriptionResponse;
import com.lcs.lxp.subscription.application.service.SubscriptionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping
    public ResponseEntity<SubscriptionResponse> create(Authentication authentication) {
        Long memberId = Long.parseLong(authentication.getName());
        SubscriptionResponse response = subscriptionService.createSubscription(memberId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{subscriptionId}")
    public ResponseEntity<SubscriptionResponse> get(@PathVariable Long subscriptionId) {
        return ResponseEntity.ok(subscriptionService.getSubscriptionInfo(subscriptionId));
    }

    @PostMapping("/{subscriptionId}/cancel")
    public ResponseEntity<Void> cancel(Authentication authentication, @PathVariable Long subscriptionId) {
        Long memberId = Long.parseLong(authentication.getName());
        subscriptionService.cancelSubscription(memberId, subscriptionId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reissue")
    public ResponseEntity<Void> reissue() {
        subscriptionService.reissueExpiring();
        return ResponseEntity.ok().build();
    }
}
