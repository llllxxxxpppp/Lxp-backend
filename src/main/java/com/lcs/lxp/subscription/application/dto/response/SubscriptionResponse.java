package com.lcs.lxp.subscription.application.dto.response;

import com.lcs.lxp.subscription.domain.model.entity.Subscription;
import java.time.OffsetDateTime;

public record SubscriptionResponse(
        Long subscriptionId,
        Long memberId,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime expiresAt,
        OffsetDateTime activatedAt
) {

    public static SubscriptionResponse from(Subscription subscription) {
        return new SubscriptionResponse(
                subscription.getId().value(),
                subscription.getMemberId(),
                subscription.getStatus().name(),
                subscription.getCreatedAt(),
                subscription.getExpiresAt(),
                subscription.getActivatedAt()
        );
    }
}
