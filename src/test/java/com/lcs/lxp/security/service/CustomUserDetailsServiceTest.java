package com.lcs.lxp.security.service;

import com.lcs.lxp.member.model.entity.InstructorMember;
import com.lcs.lxp.member.model.entity.Member;
import com.lcs.lxp.member.model.entity.RegularMember;
import com.lcs.lxp.member.repository.MemberRepository;
import com.lcs.lxp.security.principal.CustomUserPrincipal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomUserDetailsServiceTest {

    private static final String EMAIL = "user@example.com";
    private static final String PASSWORD = "encoded-password";
    private static final Long MEMBER_ID = 1L;

    private MemberRepository memberRepository;
    private CustomUserDetailsService customUserDetailsService;

    @BeforeEach
    void setUp() {
        memberRepository = mock(MemberRepository.class);
        customUserDetailsService = new CustomUserDetailsService(memberRepository);
    }

    // -------------------------------------------------------------------------
    // Existing email
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("존재하는 이메일로 조회하면 회원의 id/email/password/ROLE 권한을 가진 CustomUserPrincipal이 생성된다")
    void givenExistingEmail_whenLoadUserByUsername_thenReturnsCustomUserPrincipalWithMemberDetails() {
        RegularMember member = RegularMember.create(EMAIL, PASSWORD);
        ReflectionTestUtils.setField(member, "id", MEMBER_ID);
        when(memberRepository.findByEmail(EMAIL)).thenReturn(Optional.of(member));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(EMAIL);

        assertInstanceOf(CustomUserPrincipal.class, userDetails);
        CustomUserPrincipal principal = (CustomUserPrincipal) userDetails;
        assertEquals(MEMBER_ID, principal.getUserId());
        assertEquals(EMAIL, principal.getUsername());
        assertEquals(PASSWORD, principal.getPassword());
        assertTrue(principal.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_MEMBER".equals(authority.getAuthority())));
        verify(memberRepository).findByEmail(EMAIL);
    }

    @Test
    @DisplayName("강사 회원이면 ROLE_INSTRUCTOR 권한을 가진 CustomUserPrincipal이 생성된다")
    void givenExistingInstructorEmail_whenLoadUserByUsername_thenReturnsPrincipalWithInstructorRole() {
        Member instructor = InstructorMember.create(EMAIL, PASSWORD, "홍길동", null, null);
        ReflectionTestUtils.setField(instructor, "id", MEMBER_ID);
        when(memberRepository.findByEmail(EMAIL)).thenReturn(Optional.of(instructor));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(EMAIL);

        CustomUserPrincipal principal = (CustomUserPrincipal) userDetails;
        assertTrue(principal.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_INSTRUCTOR".equals(authority.getAuthority())));
        verify(memberRepository).findByEmail(EMAIL);
    }

    // -------------------------------------------------------------------------
    // Non-existing email
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("존재하지 않는 이메일로 조회하면 UsernameNotFoundException이 발생한다")
    void givenNonExistingEmail_whenLoadUserByUsername_thenThrowsUsernameNotFoundException() {
        when(memberRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername(EMAIL));

        verify(memberRepository).findByEmail(EMAIL);
    }
}
