package com.lcs.lxp.subscription.domain.model.vo;

import java.util.Objects;

public record SubscriptionId(Long value) {

    public SubscriptionId {
        Objects.requireNonNull(value, "SubscriptionId는 null일 수 없습니다.");
    }
}
