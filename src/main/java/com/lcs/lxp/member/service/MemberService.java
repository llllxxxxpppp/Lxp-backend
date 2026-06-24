package com.lcs.lxp.member.service;

import com.lcs.lxp.member.dto.response.TokenResponseDTO;
import com.lcs.lxp.member.dto.response.UserResponseDTO;
import com.lcs.lxp.member.exception.MemberException;
import com.lcs.lxp.member.model.entity.Member;
import com.lcs.lxp.member.model.entity.RegularMember;
import com.lcs.lxp.member.repository.MemberRepository;
import com.lcs.lxp.security.jwt.JwtTokenProvider;
import com.lcs.lxp.security.refresh.RefreshToken;
import com.lcs.lxp.security.refresh.RefreshTokenRepository;
import java.time.Instant;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberService {
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public MemberService(
            AuthenticationManager authenticationManager,
            JwtTokenProvider jwtTokenProvider,
            PasswordEncoder passwordEncoder,
            MemberRepository memberRepository,
            RefreshTokenRepository refreshTokenRepository
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.memberRepository = memberRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional
    public TokenResponseDTO login(String email, String password) {
        Authentication authentication =
                authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                        email,
                        password));

        String accessToken = jwtTokenProvider.createAccessToken(authentication);
        String newRefreshTokenValue = jwtTokenProvider.createRefreshToken();

        refreshTokenRepository.findByEmail(authentication.getName())
                .ifPresent(refreshTokenRepository::delete);

        RefreshToken newRefreshToken = new RefreshToken(
                authentication.getName(),
                newRefreshTokenValue,
                Instant.now().plusMillis(jwtTokenProvider.getRefreshTokenValidityMilliseconds()));

        refreshTokenRepository.save(newRefreshToken);

        return new TokenResponseDTO(accessToken, newRefreshTokenValue);
    }

    @Transactional
    public UserResponseDTO register(String email, String password) {
        if (memberRepository.existsByEmail(email)) {
            throw new MemberException("이미 사용 중인 이메일 입니다.");
        }

        RegularMember member = RegularMember.create(email, passwordEncoder.encode(password));

        Member savedUser = memberRepository.save(member);

        return UserResponseDTO.from(savedUser);
    }

    @Transactional
    public void logout(String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            return;
        }

        refreshTokenRepository.findByToken(refreshTokenValue)
                .ifPresent(refreshTokenRepository::delete);
    }
}
