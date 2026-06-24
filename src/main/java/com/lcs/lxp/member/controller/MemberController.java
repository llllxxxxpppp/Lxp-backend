package com.lcs.lxp.member.controller;

import com.lcs.lxp.member.dto.request.LoginRequestDTO;
import com.lcs.lxp.member.dto.request.SignupRequestDTO;
import com.lcs.lxp.member.dto.response.TokenResponseDTO;
import com.lcs.lxp.member.dto.response.UserResponseDTO;
import com.lcs.lxp.member.service.MemberService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class MemberController {
    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponseDTO> login(@RequestBody @Valid LoginRequestDTO requestDTO) {
        TokenResponseDTO tokenResponseDTO =
                memberService.login(requestDTO.email(), requestDTO.password());

        return ResponseEntity.ok(tokenResponseDTO);
    }

    @PostMapping("/signup")
    public ResponseEntity<UserResponseDTO> signup(@RequestBody @Valid SignupRequestDTO requestDTO) {
        UserResponseDTO signResponse =
                memberService.register(requestDTO.email(), requestDTO.password());

        return ResponseEntity.status(HttpStatus.CREATED).body(signResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = "X-Refresh-Token", required = false) String refreshToken
    ) {
        memberService.logout(refreshToken);
        return ResponseEntity.noContent().build();
    }
}
