# GateFlow — System Overview

## 1. Problem Statement

GateFlow is an API Management and Traffic Control Platform designed to centralize API access management, authentication, rate limiting, quota enforcement and usage tracking.

Without a centralized platform, individual backend services must implement these capabilities independently. This creates code duplication, inconsistent security policies and maintenance overhead.

GateFlow provides a centralized solution through which organizations can register APIs, manage developers, issue API keys, configure access policies and monitor API usage.

## 2. System Architecture

GateFlow follows a two-plane architecture:

### Control Plane

The Control Plane manages the platform's configuration and administrative operations.

Responsibilities:
- Organization and developer management.
- Backend API registration.
- Application and subscription management.
- API key generation and revocation.
- Rate-limit and quota configuration.
- Audit and administrative operations.

### Data Plane

The Data Plane handles incoming API traffic and enforces the policies configured through the Control Plane.

Responsibilities:
- API key validation.
- Subscription and access validation.
- Rate-limit enforcement.
- Usage quota enforcement.
- Request routing and forwarding.
- Usage event generation.

## 3. Infrastructure Components

### PostgreSQL

Used for persistent storage, including organizations, applications, API registrations, API key hashes, subscriptions, policies and audit records.

### Redis

Used for fast shared data access, including rate-limit counters, quota counters and cached configurations.

Redis also supports atomic operations that help maintain accurate counters across multiple Gateway instances.

## 4. API Request Lifecycle

1. A client sends an API request with an API key.
2. The Gateway validates the API key.
3. The Gateway identifies the associated application.
4. The Gateway checks the application's subscription and access permissions.
5. The Gateway checks rate limits and usage quotas.
6. If the request is valid, it forwards the request to the registered backend API.
7. GateFlow records usage information for analytics and auditing.
8. If the request exceeds its rate limit, the Gateway responds with HTTP 429.

## 5. Architectural Goal

The primary goal is to provide a centralized, secure and scalable platform for managing API access and controlling traffic without requiring every backend service to implement these capabilities independently.