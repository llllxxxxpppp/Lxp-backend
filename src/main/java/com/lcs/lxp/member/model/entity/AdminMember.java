package com.lcs.lxp.member.model.entity;

import com.lcs.lxp.member.model.MemberRole;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("ADMIN")
public class AdminMember extends Member {

    protected AdminMember() {}

    private AdminMember(String email, String encodedPassword) {
        super(email, encodedPassword);
    }

    public static AdminMember create(String email, String encodedPassword) {
        return new AdminMember(email, encodedPassword);
    }

    @Override
    public MemberRole getRole() {
        return MemberRole.ADMIN;
    }
}
