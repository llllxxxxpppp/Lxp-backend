package com.lcs.lxp.member.service;

import com.lcs.lxp.member.exception.MemberException;
import com.lcs.lxp.member.model.entity.InstructorMember;
import com.lcs.lxp.member.model.entity.RegularMember;
import com.lcs.lxp.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private com.lcs.lxp.security.jwt.JwtTokenProvider jwtTokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private com.lcs.lxp.security.refresh.RefreshTokenRepository refreshTokenRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private MemberService memberService;

    // -------------------------------------------------------------------------
    // suspendMember
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("일반 회원을 정지시키면 save와 publishEvent가 호출된다")
    void givenExistingRegularMember_whenSuspendMember_thenSaveAndPublishEventAreInvoked() {
        Long memberId = 1L;
        RegularMember regularMember = RegularMember.create("user@example.com", "encoded_password");

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(regularMember));

        memberService.suspendMember(memberId);

        verify(memberRepository).save(any(RegularMember.class));
        verify(applicationEventPublisher).publishEvent(argThat((Object event) ->
                event instanceof com.lcs.lxp.member.event.MemberSuspendedEvent
        ));
    }

    @Test
    @DisplayName("존재하지 않는 일반 회원을 정지시키려 하면 MemberException이 발생한다")
    void givenNonExistingMember_whenSuspendMember_thenThrowsMemberException() {
        Long memberId = 999L;

        when(memberRepository.findById(memberId)).thenReturn(Optional.empty());

        assertThrows(MemberException.class, () -> memberService.suspendMember(memberId));

        verify(memberRepository, never()).save(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("강사를 일반 회원으로 정지시키려 하면 MemberException이 발생한다")
    void givenInstructorMember_whenSuspendMember_thenThrowsMemberException() {
        Long instructorId = 1L;
        InstructorMember instructorMember = InstructorMember.create("instructor@example.com", "encoded_password",
                "홍길동", null, null);

        when(memberRepository.findById(instructorId)).thenReturn(Optional.of(instructorMember));

        assertThrows(MemberException.class, () -> memberService.suspendMember(instructorId));

        verify(memberRepository, never()).save(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    // -------------------------------------------------------------------------
    // withdrawMember
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("일반 회원을 탈퇴시키면 save와 publishEvent가 호출된다")
    void givenExistingRegularMember_whenWithdrawMember_thenSaveAndPublishEventAreInvoked() {
        Long memberId = 1L;
        RegularMember regularMember = RegularMember.create("user@example.com", "encoded_password");

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(regularMember));

        memberService.withdrawMember(memberId);

        verify(memberRepository).save(any(RegularMember.class));
        verify(applicationEventPublisher).publishEvent(argThat((Object event) ->
                event instanceof com.lcs.lxp.member.event.MemberWithdrawnEvent
        ));
    }

    @Test
    @DisplayName("존재하지 않는 회원을 탈퇴시키려 하면 MemberException이 발생한다")
    void givenNonExistingMember_whenWithdrawMember_thenThrowsMemberException() {
        Long memberId = 999L;

        when(memberRepository.findById(memberId)).thenReturn(Optional.empty());

        assertThrows(MemberException.class, () -> memberService.withdrawMember(memberId));

        verify(memberRepository, never()).save(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("강사를 일반 회원 탈퇴로 처리하려 하면 MemberException이 발생한다")
    void givenInstructorMember_whenWithdrawMember_thenThrowsMemberException() {
        Long instructorId = 1L;
        InstructorMember instructorMember = InstructorMember.create("instructor@example.com", "encoded_password",
                "홍길동", null, null);

        when(memberRepository.findById(instructorId)).thenReturn(Optional.of(instructorMember));

        assertThrows(MemberException.class, () -> memberService.withdrawMember(instructorId));

        verify(memberRepository, never()).save(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    // -------------------------------------------------------------------------
    // suspendInstructor
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("강사를 정지시키면 save와 publishEvent가 호출된다")
    void givenExistingInstructor_whenSuspendInstructor_thenSaveAndPublishEventAreInvoked() {
        Long instructorId = 1L;
        InstructorMember instructorMember = InstructorMember.create("instructor@example.com", "encoded_password",
                "홍길동", null, null);

        when(memberRepository.findById(instructorId)).thenReturn(Optional.of(instructorMember));

        memberService.suspendInstructor(instructorId);

        verify(memberRepository).save(any(InstructorMember.class));
        verify(applicationEventPublisher).publishEvent(argThat((Object event) ->
                event instanceof com.lcs.lxp.member.event.InstructorSuspendedEvent
        ));
    }

    @Test
    @DisplayName("존재하지 않는 강사를 정지시키려 하면 MemberException이 발생한다")
    void givenNonExistingInstructor_whenSuspendInstructor_thenThrowsMemberException() {
        Long instructorId = 999L;

        when(memberRepository.findById(instructorId)).thenReturn(Optional.empty());

        assertThrows(MemberException.class, () -> memberService.suspendInstructor(instructorId));

        verify(memberRepository, never()).save(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("일반 회원을 강사로 정지시키려 하면 MemberException이 발생한다")
    void givenRegularMember_whenSuspendInstructor_thenThrowsMemberException() {
        Long memberId = 1L;
        RegularMember regularMember = RegularMember.create("user@example.com", "encoded_password");

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(regularMember));

        assertThrows(MemberException.class, () -> memberService.suspendInstructor(memberId));

        verify(memberRepository, never()).save(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    // -------------------------------------------------------------------------
    // registerInstructor
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("강사를 정상적으로 등록하면 save가 호출되고 UserResponseDTO를 반환한다")
    void givenValidInstructorData_whenRegisterInstructor_thenSaveIsCalledAndReturnUserResponseDTO() {
        String email = "instructor@example.com";
        String password = "password123";
        String name = "홍길동";
        String profileImageUrl = "https://example.com/image.jpg";
        String introduction = "안녕하세요, 저는 홍길동입니다.";

        InstructorMember instructorMember = InstructorMember.create(email, "encoded_password", name, profileImageUrl, introduction);
        ReflectionTestUtils.setField(instructorMember, "id", 1L);

        when(memberRepository.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn("encoded_password");
        when(memberRepository.save(any(InstructorMember.class))).thenReturn(instructorMember);

        com.lcs.lxp.member.dto.response.UserResponseDTO result = memberService.registerInstructor(
                email, password, name, profileImageUrl, introduction);

        verify(memberRepository).existsByEmail(email);
        verify(passwordEncoder).encode(password);
        verify(memberRepository).save(any(InstructorMember.class));

        assertEquals(1L, result.id());
        assertEquals(email, result.email());
        assertEquals(com.lcs.lxp.member.model.MemberRole.INSTRUCTOR, result.role());
    }

    @Test
    @DisplayName("이미 존재하는 이메일로 강사를 등록하려 하면 MemberException이 발생한다")
    void givenExistingEmail_whenRegisterInstructor_thenThrowsMemberException() {
        String email = "instructor@example.com";
        String password = "password123";
        String name = "홍길동";
        String profileImageUrl = "https://example.com/image.jpg";
        String introduction = "안녕하세요, 저는 홍길동입니다.";

        when(memberRepository.existsByEmail(email)).thenReturn(true);

        assertThrows(MemberException.class, () ->
                memberService.registerInstructor(email, password, name, profileImageUrl, introduction));

        verify(memberRepository).existsByEmail(email);
        verify(memberRepository, never()).save(any());
    }
}
