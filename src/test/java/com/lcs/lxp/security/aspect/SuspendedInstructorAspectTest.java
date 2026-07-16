package com.lcs.lxp.security.aspect;

import com.lcs.lxp.member.model.entity.InstructorMember;
import com.lcs.lxp.member.repository.MemberRepository;
import com.lcs.lxp.security.exception.SuspendedInstructorException;
import com.lcs.lxp.security.principal.CustomUserPrincipal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link SuspendedInstructorAspect}의 판단 로직(회원 정지 여부 확인 후 예외 발생 여부)을 검증하는
 * 순수 Mockito 단위 테스트이다.
 *
 * <p>이 테스트는 {@code @ExtendWith(MockitoExtension.class)} 환경에서 애스펙트 객체를
 * 직접 생성하여 {@code rejectIfSuspendedInstructor()} 어드바이스 메서드를 호출하는 방식으로 작성되었다.
 * Mockito는 스프링 AOP 프록시를 생성하지 않으므로, {@code @RejectSuspendedInstructor} 애노테이션이
 * 실제 {@code CourseService} 메서드 호출 시점에 인터셉트되어 이 어드바이스를 실행하는지(즉 애스펙트가
 * 실제로 "짜여드는지")는 이 테스트만으로 검증할 수 없다 — 그 부분은 별도의
 * {@code SuspendedInstructorAspectIntegrationTest}(@SpringBootTest)가 검증한다.
 * 이 테스트는 애스펙트의 판단 로직 자체(정지 여부 조회, 역할 필터링, 예외 발생 조건)만을 대상으로 한다.
 */
@ExtendWith(MockitoExtension.class)
class SuspendedInstructorAspectTest {

    private static final long SUSPENDED_INSTRUCTOR_ID = 100L;
    private static final long ACTIVE_INSTRUCTOR_ID = 200L;
    private static final long MEMBER_ID = 300L;
    private static final long UNKNOWN_INSTRUCTOR_ID = 400L;

    @Mock
    private MemberRepository memberRepository;

    private SuspendedInstructorAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new SuspendedInstructorAspect(memberRepository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("정지된 강사로 인증된 상태에서 대상 메서드를 호출하면 예외가 발생한다")
    void givenSuspendedInstructor_whenRejectIfSuspendedInstructor_thenThrowsException() {
        InstructorMember suspended = createInstructor("suspended@test.com", SUSPENDED_INSTRUCTOR_ID);
        suspended.suspend();
        when(memberRepository.findById(SUSPENDED_INSTRUCTOR_ID)).thenReturn(Optional.of(suspended));
        authenticateAs(SUSPENDED_INSTRUCTOR_ID, "ROLE_INSTRUCTOR");

        assertThrows(SuspendedInstructorException.class, () -> aspect.rejectIfSuspendedInstructor());

        verify(memberRepository).findById(SUSPENDED_INSTRUCTOR_ID);
    }

    @Test
    @DisplayName("정지되지 않은 강사로 인증된 상태에서 대상 메서드를 호출하면 예외가 발생하지 않는다")
    void givenActiveInstructor_whenRejectIfSuspendedInstructor_thenDoesNotThrow() {
        InstructorMember active = createInstructor("active@test.com", ACTIVE_INSTRUCTOR_ID);
        when(memberRepository.findById(ACTIVE_INSTRUCTOR_ID)).thenReturn(Optional.of(active));
        authenticateAs(ACTIVE_INSTRUCTOR_ID, "ROLE_INSTRUCTOR");

        assertDoesNotThrow(() -> aspect.rejectIfSuspendedInstructor());

        verify(memberRepository).findById(ACTIVE_INSTRUCTOR_ID);
    }

    @Test
    @DisplayName("일반 회원 권한으로 인증된 상태에서는 회원 조회 없이 통과한다")
    void givenMemberRole_whenRejectIfSuspendedInstructor_thenSkipsLookup() {
        authenticateAs(MEMBER_ID, "ROLE_MEMBER");

        assertDoesNotThrow(() -> aspect.rejectIfSuspendedInstructor());

        verifyNoInteractions(memberRepository);
    }

    @Test
    @DisplayName("어드민 권한으로 인증된 상태에서는 회원 조회 없이 통과한다")
    void givenAdminRole_whenRejectIfSuspendedInstructor_thenSkipsLookup() {
        authenticateAs(MEMBER_ID, "ROLE_ADMIN");

        assertDoesNotThrow(() -> aspect.rejectIfSuspendedInstructor());

        verifyNoInteractions(memberRepository);
    }

    @Test
    @DisplayName("강사 권한과 어드민 권한을 함께 가진 정지된 사용자가 호출하면 예외가 발생한다")
    void givenSuspendedUserWithInstructorAndAdminRoles_whenRejectIfSuspendedInstructor_thenThrowsException() {
        InstructorMember suspended = createInstructor("dual@test.com", SUSPENDED_INSTRUCTOR_ID);
        suspended.suspend();
        when(memberRepository.findById(SUSPENDED_INSTRUCTOR_ID)).thenReturn(Optional.of(suspended));
        authenticateAs(SUSPENDED_INSTRUCTOR_ID, "ROLE_INSTRUCTOR", "ROLE_ADMIN");

        assertThrows(SuspendedInstructorException.class, () -> aspect.rejectIfSuspendedInstructor());

        verify(memberRepository).findById(SUSPENDED_INSTRUCTOR_ID);
    }

    @Test
    @DisplayName("인증 정보가 없는 상태에서 호출하면 회원 조회 없이 통과한다")
    void givenNoAuthentication_whenRejectIfSuspendedInstructor_thenSkipsLookup() {
        SecurityContextHolder.clearContext();

        assertDoesNotThrow(() -> aspect.rejectIfSuspendedInstructor());

        verifyNoInteractions(memberRepository);
    }

    @Test
    @DisplayName("CustomUserPrincipal이 아닌 인증 주체가 강사 권한을 가진 경우 회원 조회 없이 통과한다")
    void givenNonCustomUserPrincipalWithInstructorRole_whenRejectIfSuspendedInstructor_thenSkipsLookup() {
        User genericPrincipal = new User("instructor", "", List.of(new SimpleGrantedAuthority("ROLE_INSTRUCTOR")));
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                genericPrincipal, null, genericPrincipal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertDoesNotThrow(() -> aspect.rejectIfSuspendedInstructor());

        verifyNoInteractions(memberRepository);
    }

    @Test
    @DisplayName("강사 권한을 가졌으나 회원 정보를 찾을 수 없으면 예외 없이 통과한다")
    void givenInstructorRoleButMemberNotFound_whenRejectIfSuspendedInstructor_thenDoesNotThrow() {
        when(memberRepository.findById(UNKNOWN_INSTRUCTOR_ID)).thenReturn(Optional.empty());
        authenticateAs(UNKNOWN_INSTRUCTOR_ID, "ROLE_INSTRUCTOR");

        assertDoesNotThrow(() -> aspect.rejectIfSuspendedInstructor());

        verify(memberRepository).findById(UNKNOWN_INSTRUCTOR_ID);
    }

    private InstructorMember createInstructor(String email, long id) {
        InstructorMember instructor = InstructorMember.create(email, "encoded_password", "이름", null, "소개");
        ReflectionTestUtils.setField(instructor, "id", id);
        return instructor;
    }

    private void authenticateAs(long userId, String... roles) {
        List<GrantedAuthority> authorities = Arrays.stream(roles)
                .<GrantedAuthority>map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        CustomUserPrincipal principal =
                new CustomUserPrincipal(userId, "user" + userId + "@test.com", "", authorities, false);
        Authentication authentication =
                UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
