package com.gateflow.management_api.subscription.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gateflow.management_api.subscription.entity.Subscription;

public interface SubscriptionRepository
        extends JpaRepository<Subscription, UUID> {

    Optional<Subscription> findByApplicationIdAndApiId(
            UUID applicationId,
            UUID apiId
    );
}
