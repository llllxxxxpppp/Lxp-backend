package com.lcs.lxp.member.service;

import com.lcs.lxp.member.dto.response.TokenResponseDTO;
import com.lcs.lxp.member.dto.response.UserResponseDTO;
import com.lcs.lxp.member.event.InstructorSuspendedEvent;
import com.lcs.lxp.member.event.MemberSuspendedEvent;
import com.lcs.lxp.member.event.MemberWithdrawnEvent;
import com.lcs.lxp.member.exception.MemberException;
import com.lcs.lxp.member.model.entity.InstructorMember;
import com.lcs.lxp.member.model.entity.Member;
import com.lcs.lxp.member.model.entity.RegularMember;
import com.lcs.lxp.member.repository.MemberRepository;
import com.lcs.lxp.security.jwt.JwtTokenProvider;
import com.lcs.lxp.security.refresh.RefreshToken;
import com.lcs.lxp.security.refresh.RefreshTokenRepository;
import java.time.Instant;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;

    public MemberService(
            AuthenticationManager authenticationManager,
            JwtTokenProvider jwtTokenProvider,
            PasswordEncoder passwordEncoder,
            MemberRepository memberRepository,
            RefreshTokenRepository refreshTokenRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.memberRepository = memberRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.eventPublisher = eventPublisher;
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
    public UserResponseDTO registerInstructor(
            String email,
            String password,
            String name,
            String profileImageUrl,
            String introduction
    ) {
        if (memberRepository.existsByEmail(email)) {
            throw new MemberException("이미 사용 중인 이메일 입니다.");
        }

        InstructorMember member = InstructorMember.create(
                email, passwordEncoder.encode(password), name, profileImageUrl, introduction);

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

    @Transactional
    public void suspendMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException("존재하지 않는 회원입니다."));

        if (!(member instanceof RegularMember)) {
            throw new MemberException("일반 회원만 정지시킬 수 있습니다.");
        }

        RegularMember regularMember = (RegularMember) member;
        regularMember.suspend();
        memberRepository.save(regularMember);

        eventPublisher.publishEvent(new MemberSuspendedEvent(memberId));
    }

    @Transactional
    public void withdrawMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException("존재하지 않는 회원입니다."));

        if (!(member instanceof RegularMember)) {
            throw new MemberException("일반 회원만 탈퇴할 수 있습니다.");
        }

        RegularMember regularMember = (RegularMember) member;
        regularMember.withdraw();
        memberRepository.save(regularMember);

        eventPublisher.publishEvent(new MemberWithdrawnEvent(memberId));
    }

    @Transactional
    public void suspendInstructor(Long instructorId) {
        Member member = memberRepository.findById(instructorId)
                .orElseThrow(() -> new MemberException("존재하지 않는 강사입니다."));

        if (!(member instanceof InstructorMember)) {
            throw new MemberException("강사만 정지시킬 수 있습니다.");
        }

        InstructorMember instructorMember = (InstructorMember) member;
        instructorMember.suspend();
        memberRepository.save(instructorMember);

        eventPublisher.publishEvent(new InstructorSuspendedEvent(instructorId));
    }

    @Transactional
    public void changePassword(Long memberId, String currentPassword, String newPassword) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException("존재하지 않는 회원입니다."));

        if (!passwordEncoder.matches(currentPassword, member.getPassword())) {
            throw new MemberException("현재 비밀번호가 일치하지 않습니다.");
        }

        member.updatePassword(passwordEncoder.encode(newPassword));
        memberRepository.save(member);
    }

    @Transactional
    public UserResponseDTO updateInstructorProfile(
            Long memberId,
            String name,
            String profileImageUrl,
            String introduction
    ) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException("존재하지 않는 회원입니다."));

        if (!(member instanceof InstructorMember instructorMember)) {
            throw new MemberException("강사만 프로필을 변경할 수 있습니다.");
        }

        instructorMember.updateProfile(name, profileImageUrl, introduction);
        Member savedMember = memberRepository.save(instructorMember);

        return UserResponseDTO.from(savedMember);
    }
}
