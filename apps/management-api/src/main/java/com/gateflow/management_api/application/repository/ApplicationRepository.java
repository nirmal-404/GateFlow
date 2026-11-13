package com.gateflow.management_api.application.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gateflow.management_api.application.entity.Application;

public interface ApplicationRepository
        extends JpaRepository<Application, UUID> {
}
