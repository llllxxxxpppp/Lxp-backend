package com.lcs.lxp.member.model.entity;

import com.lcs.lxp.member.model.MemberRole;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.time.OffsetDateTime;

@Entity
@DiscriminatorValue("MEMBER")
public class RegularMember extends Member {

    @Column
    private OffsetDateTime withdrawnAt;

    protected RegularMember() {}

    private RegularMember(String email, String encodedPassword) {
        super(email, encodedPassword);
    }

    public static RegularMember create(String email, String encodedPassword) {
        return new RegularMember(email, encodedPassword);
    }

    @Override
    public MemberRole getRole() {
        return MemberRole.MEMBER;
    }

    public OffsetDateTime getWithdrawnAt() {
        return withdrawnAt;
    }

    public void withdraw() {
        markDeleted();
        this.withdrawnAt = OffsetDateTime.now();
    }

    public void suspend() {
        markSuspended();
        markDeleted();
    }
}
