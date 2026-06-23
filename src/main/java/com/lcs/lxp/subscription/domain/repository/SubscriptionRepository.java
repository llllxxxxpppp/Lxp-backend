package com.lcs.lxp.subscription.domain.repository;

import com.lcs.lxp.subscription.domain.model.entity.Subscription;
import com.lcs.lxp.subscription.domain.model.vo.SubscriptionStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    boolean existsByMemberId(Long memberId);

    Optional<Subscription> findByMemberIdAndStatus(Long memberId, SubscriptionStatus status);

    List<Subscription> findByStatusAndExpiresAtBetween(SubscriptionStatus status, OffsetDateTime from, OffsetDateTime to);
}
