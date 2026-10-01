package com.w2m.backend.notification.repository;

import com.w2m.backend.notification.entity.PushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {

    List<PushSubscription> findByUserId(Long userId);

    List<PushSubscription> findByUserIdIn(Collection<Long> userIds);

    Optional<PushSubscription> findByEndpoint(String endpoint);

    void deleteByEndpointIn(Collection<String> endpoints);
}
