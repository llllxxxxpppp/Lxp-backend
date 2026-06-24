package com.lcs.lxp.member.dto.response;

import com.lcs.lxp.member.model.MemberRole;
import com.lcs.lxp.member.model.entity.Member;

public record UserResponseDTO(Long id, String email, MemberRole role) {
    public static UserResponseDTO from(Member member) {
        return new UserResponseDTO(member.getId().value(), member.getEmail(), member.getRole());
    }
}
