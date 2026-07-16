package com.lcs.lxp.subscription.domain.repository;

import com.lcs.lxp.subscription.domain.model.entity.Subscription;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    boolean existsByMemberId(Long memberId);

    List<Subscription> findByMemberId(Long memberId);

    List<Subscription> findByActivatedAtIsNotNullAndSuspendedAtIsNullAndCancelledAtIsNull();
}
