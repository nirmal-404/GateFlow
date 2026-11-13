package com.gateflow.management_api.subscription.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.gateflow.management_api.subscription.dto.SubscriptionResponse;
import com.gateflow.management_api.subscription.service.SubscriptionService;

@RestController
@RequestMapping("/api/v1/applications/{applicationId}/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping
    public List<SubscriptionResponse> getSubscriptionsByApplication(
            @PathVariable UUID applicationId) {

        return subscriptionService.getSubscriptionsByApplication(applicationId);
    }

    @PostMapping("/{apiId}")
    @ResponseStatus(HttpStatus.CREATED)
    public SubscriptionResponse subscribe(
            @PathVariable UUID applicationId,
            @PathVariable UUID apiId
    ) {
        return subscriptionService.subscribe(applicationId, apiId);
    }

    @PatchMapping("/{subscriptionId}/suspend")
    public SubscriptionResponse suspendSubscription(
            @PathVariable UUID applicationId,
            @PathVariable UUID subscriptionId
    ) {

        return subscriptionService.suspendSubscription(applicationId, subscriptionId);
    }

    @PatchMapping("/{subscriptionId}/revoke")
    public SubscriptionResponse revokeSubscription(
            @PathVariable UUID applicationId,
            @PathVariable UUID subscriptionId
    ) {

        return subscriptionService.revokeSubscription(applicationId, subscriptionId);
    }

    @PatchMapping("/{subscriptionId}/reactivate")
    public SubscriptionResponse reactivateSubscription(
            @PathVariable UUID applicationId,
            @PathVariable UUID subscriptionId
    ) {

        return subscriptionService.reactivateSubscription(applicationId, subscriptionId);
    }
}
