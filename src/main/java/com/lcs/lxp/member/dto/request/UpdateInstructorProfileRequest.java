package com.lcs.lxp.member.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateInstructorProfileRequest(
        @NotBlank String name,
        String profileImageUrl,
        String introduction) {
}
