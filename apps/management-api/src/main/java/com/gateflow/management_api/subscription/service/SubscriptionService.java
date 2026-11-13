package com.gateflow.management_api.subscription.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gateflow.management_api.api.entity.Api;
import com.gateflow.management_api.api.exception.ApiNotFoundException;
import com.gateflow.management_api.application.entity.Application;
import com.gateflow.management_api.application.exception.ApplicationNotFoundException;
import com.gateflow.management_api.api.repository.ApiRepository;
import com.gateflow.management_api.application.repository.ApplicationRepository;
import com.gateflow.management_api.subscription.dto.SubscriptionResponse;
import com.gateflow.management_api.subscription.entity.Subscription;
import com.gateflow.management_api.subscription.entity.SubscriptionStatus;
import com.gateflow.management_api.subscription.exception.SubscriptionAlreadyExistsException;
import com.gateflow.management_api.subscription.exception.SubscriptionNotAllowedException;
import com.gateflow.management_api.subscription.repository.SubscriptionRepository;

@Service
@Transactional
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final ApplicationRepository applicationRepository;
    private final ApiRepository apiRepository;

    public SubscriptionService(
            SubscriptionRepository subscriptionRepository,
            ApplicationRepository applicationRepository,
            ApiRepository apiRepository
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.applicationRepository = applicationRepository;
        this.apiRepository = apiRepository;
    }

    public SubscriptionResponse subscribe(UUID applicationId, UUID apiId) {

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(
                        "Application not found: " + applicationId
                ));

        Api api = apiRepository.findById(apiId)
                .orElseThrow(() -> new ApiNotFoundException(
                        "API not found: " + apiId
                ));

        UUID applicationOrganizationId
                = application.getOrganization().getId();

        UUID apiOrganizationId
                = api.getOrganization().getId();

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

        Subscription savedSubscription
                = subscriptionRepository.save(subscription);

        return new SubscriptionResponse(
                savedSubscription.getId(),
                savedSubscription.getOrganization().getId(),
                savedSubscription.getApplication().getId(),
                savedSubscription.getApplication().getName(),
                savedSubscription.getApi().getId(),
                savedSubscription.getApi().getName(),
                savedSubscription.getStatus(),
                savedSubscription.getCreatedAt(),
                savedSubscription.getUpdatedAt()
        );
    }
}
