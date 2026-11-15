package com.gateflow.management_api.subscription.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gateflow.management_api.api.entity.Api;
import com.gateflow.management_api.api.exception.ApiNotFoundException;
import com.gateflow.management_api.api.repository.ApiRepository;
import com.gateflow.management_api.application.entity.Application;
import com.gateflow.management_api.application.exception.ApplicationNotFoundException;
import com.gateflow.management_api.application.repository.ApplicationRepository;
import com.gateflow.management_api.security.authorization.OrganizationAuthorizationService;
import com.gateflow.management_api.security.authorization.OrganizationPermission;
import com.gateflow.management_api.subscription.dto.SubscriptionResponse;
import com.gateflow.management_api.subscription.entity.Subscription;
import com.gateflow.management_api.subscription.entity.SubscriptionStatus;
import com.gateflow.management_api.subscription.exception.InvalidSubscriptionStateException;
import com.gateflow.management_api.subscription.exception.SubscriptionAlreadyExistsException;
import com.gateflow.management_api.subscription.exception.SubscriptionNotAllowedException;
import com.gateflow.management_api.subscription.exception.SubscriptionNotFoundException;
import com.gateflow.management_api.subscription.repository.SubscriptionRepository;

@Service
@Transactional
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final ApplicationRepository applicationRepository;
    private final ApiRepository apiRepository;
    private final OrganizationAuthorizationService authorizationService;

    public SubscriptionService(
            SubscriptionRepository subscriptionRepository,
            ApplicationRepository applicationRepository,
            ApiRepository apiRepository,
            OrganizationAuthorizationService authorizationService
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.applicationRepository = applicationRepository;
        this.apiRepository = apiRepository;
        this.authorizationService = authorizationService;
    }

    public SubscriptionResponse subscribe(UUID applicationId, UUID apiId) {

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(
                        "Application not found: " + applicationId
                ));

        authorizationService.requirePermission(
                application.getOrganization().getId(),
                OrganizationPermission.MANAGE_SUBSCRIPTIONS
        );

        Api api = apiRepository.findById(apiId)
                .orElseThrow(() -> new ApiNotFoundException(
                        "API not found: " + apiId
                ));

        UUID applicationOrganizationId = application.getOrganization().getId();
        UUID apiOrganizationId = api.getOrganization().getId();

        if (!applicationOrganizationId.equals(apiOrganizationId)) {
            throw new SubscriptionNotAllowedException(
                    "Application and API must belong to the same organization"
            );
        }

        var existingSubscription = subscriptionRepository
                .findByApplicationIdAndApiId(applicationId, apiId);

        Subscription subscription;

        if (existingSubscription.isPresent()) {

            subscription = existingSubscription.get();

            if (subscription.getStatus() == SubscriptionStatus.REVOKED) {
                subscription.setStatus(SubscriptionStatus.ACTIVE);
            } else {
                throw new SubscriptionAlreadyExistsException(
                        "Application is already subscribed to this API"
                );
            }

        } else {
            subscription = new Subscription(
                    application.getOrganization(),
                    application,
                    api
            );
        }

        return toResponse(subscriptionRepository.save(subscription));
    }

    public SubscriptionResponse suspendSubscription(
            UUID applicationId,
            UUID subscriptionId
    ) {
        Subscription subscription = findSubscription(applicationId, subscriptionId);

        authorizationService.requirePermission(
                subscription.getOrganization().getId(),
                OrganizationPermission.MANAGE_SUBSCRIPTIONS
        );

        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new InvalidSubscriptionStateException(
                    "Only active subscriptions can be suspended"
            );
        }

        subscription.setStatus(SubscriptionStatus.SUSPENDED);

        return toResponse(subscriptionRepository.save(subscription));
    }

    public SubscriptionResponse revokeSubscription(
            UUID applicationId,
            UUID subscriptionId
    ) {
        Subscription subscription = findSubscription(applicationId, subscriptionId);

        authorizationService.requirePermission(
                subscription.getOrganization().getId(),
                OrganizationPermission.MANAGE_SUBSCRIPTIONS
        );

        if (subscription.getStatus() == SubscriptionStatus.REVOKED) {
            throw new InvalidSubscriptionStateException(
                    "Subscription is already revoked"
            );
        }

        subscription.setStatus(SubscriptionStatus.REVOKED);

        return toResponse(subscriptionRepository.save(subscription));
    }

    public SubscriptionResponse reactivateSubscription(
            UUID applicationId,
            UUID subscriptionId
    ) {
        Subscription subscription = findSubscription(applicationId, subscriptionId);

        authorizationService.requirePermission(
                subscription.getOrganization().getId(),
                OrganizationPermission.MANAGE_SUBSCRIPTIONS
        );

        if (subscription.getStatus() != SubscriptionStatus.SUSPENDED) {
            throw new InvalidSubscriptionStateException(
                    "Only suspended subscriptions can be reactivated"
            );
        }

        subscription.setStatus(SubscriptionStatus.ACTIVE);

        return toResponse(subscriptionRepository.save(subscription));
    }

    @Transactional(readOnly = true)
    public List<SubscriptionResponse> getSubscriptionsByApplication(
            UUID applicationId
    ) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(
                        "Application not found: " + applicationId
                ));

        authorizationService.requirePermission(
                application.getOrganization().getId(),
                OrganizationPermission.VIEW_SUBSCRIPTIONS
        );

        return subscriptionRepository.findAllByApplicationId(applicationId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private Subscription findSubscription(
            UUID applicationId,
            UUID subscriptionId
    ) {
        return subscriptionRepository.findByIdAndApplicationId(
                subscriptionId,
                applicationId
        ).orElseThrow(() -> new SubscriptionNotFoundException(
                "Subscription not found for this application"
        ));
    }

    private SubscriptionResponse toResponse(Subscription subscription) {
        return new SubscriptionResponse(
                subscription.getId(),
                subscription.getOrganization().getId(),
                subscription.getApplication().getId(),
                subscription.getApplication().getName(),
                subscription.getApi().getId(),
                subscription.getApi().getName(),
                subscription.getStatus(),
                subscription.getCreatedAt(),
                subscription.getUpdatedAt()
        );
    }
}
