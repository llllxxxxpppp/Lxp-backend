package com.lcs.lxp.member.event;

import com.lcs.lxp.common.event.BaseDomainEvent;

public class MemberSuspendedEvent extends BaseDomainEvent {

    private final Long memberId;

    public MemberSuspendedEvent(Long memberId) {
        super();
        this.memberId = memberId;
    }

    public Long getMemberId() {
        return memberId;
    }
}
