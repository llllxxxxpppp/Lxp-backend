package com.lcs.lxp.member.controller;

import com.lcs.lxp.member.dto.request.ChangePasswordRequest;
import com.lcs.lxp.member.dto.request.UpdateInstructorProfileRequest;
import com.lcs.lxp.member.dto.response.UserResponseDTO;
import com.lcs.lxp.member.service.MemberService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members/me")
public class MemberSelfController {

    private final MemberService memberService;

    public MemberSelfController(MemberService memberService) {
        this.memberService = memberService;
    }

    @PatchMapping("/password")
    public ResponseEntity<Void> changePassword(
            @RequestBody @Valid ChangePasswordRequest request,
            Authentication authentication) {
        Long memberId = Long.parseLong(authentication.getName());
        memberService.changePassword(memberId, request.currentPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/instructor-profile")
    public ResponseEntity<UserResponseDTO> updateInstructorProfile(
            @RequestBody @Valid UpdateInstructorProfileRequest request,
            Authentication authentication) {
        Long memberId = Long.parseLong(authentication.getName());
        UserResponseDTO response = memberService.updateInstructorProfile(
                memberId, request.name(), request.profileImageUrl(), request.introduction());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    public ResponseEntity<Void> withdraw(Authentication authentication) {
        Long memberId = Long.parseLong(authentication.getName());
        memberService.withdrawMember(memberId);
        return ResponseEntity.noContent().build();
    }
}
