package com.gateflow.management_api.subscription.controller;

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

    @PostMapping("/{apiId}")
    @ResponseStatus(HttpStatus.CREATED)
    public SubscriptionResponse subscribe(
            @PathVariable UUID applicationId,
            @PathVariable UUID apiId
    ) {
        return subscriptionService.subscribe(applicationId, apiId);
    }
}
