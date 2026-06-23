package com.lcs.lxp.member.model.entity;

import com.lcs.lxp.member.model.MemberRole;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("MEMBER")
public class RegularMember extends Member {

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

    public void withdraw() {
        markDeleted();
    }
}
