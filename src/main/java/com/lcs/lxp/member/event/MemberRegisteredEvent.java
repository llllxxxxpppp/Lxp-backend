package com.lcs.lxp.member.event;

import com.lcs.lxp.common.event.BaseDomainEvent;

public class MemberRegisteredEvent extends BaseDomainEvent {

    private final Long memberId;

    public MemberRegisteredEvent(Long memberId) {
        super();
        this.memberId = memberId;
    }

    public Long getMemberId() {
        return memberId;
    }
}
