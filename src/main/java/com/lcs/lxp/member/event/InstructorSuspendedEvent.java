package com.lcs.lxp.member.event;

import com.lcs.lxp.common.event.BaseDomainEvent;

public class InstructorSuspendedEvent extends BaseDomainEvent {

    private final Long instructorId;

    public InstructorSuspendedEvent(Long instructorId) {
        super();
        this.instructorId = instructorId;
    }

    public Long getInstructorId() {
        return instructorId;
    }
}
