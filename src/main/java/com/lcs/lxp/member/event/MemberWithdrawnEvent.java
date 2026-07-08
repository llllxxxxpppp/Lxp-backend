package com.lcs.lxp.member.event;

import com.lcs.lxp.common.event.BaseDomainEvent;

public class MemberWithdrawnEvent extends BaseDomainEvent {

    private final Long memberId;

    public MemberWithdrawnEvent(Long memberId) {
        super();
        this.memberId = memberId;
    }

    public Long getMemberId() {
        return memberId;
    }
}
