package com.lcs.lxp.security.aspect;

import com.lcs.lxp.member.model.entity.Member;
import com.lcs.lxp.member.repository.MemberRepository;
import com.lcs.lxp.security.exception.SuspendedInstructorException;
import com.lcs.lxp.security.principal.CustomUserPrincipal;
import java.util.Optional;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * COURSE-08b: 정지된 강사 2차 방어 인터셉터(AOP).
 *
 * <p>JWT는 매 요청마다 DB 최신 정지 상태를 재확인하지 않으므로,
 * {@code @RejectSuspendedInstructor}가 부착된 메서드 호출 직전에 인증된 사용자가
 * 정지된 강사인지 DB를 재조회하여 확인한다.
 */
@Aspect
@Component
public class SuspendedInstructorAspect {

    private static final String INSTRUCTOR_AUTHORITY = "ROLE_INSTRUCTOR";

    private final MemberRepository memberRepository;

    public SuspendedInstructorAspect(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Before("@annotation(com.lcs.lxp.security.aspect.RejectSuspendedInstructor)")
    public void rejectIfSuspendedInstructor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return;
        }
        if (!hasInstructorAuthority(authentication)) {
            return;
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof CustomUserPrincipal customUserPrincipal)) {
            return;
        }

        Optional<Member> member = memberRepository.findById(customUserPrincipal.getUserId());
        if (member.isEmpty()) {
            return;
        }
        if (member.get().isSuspended()) {
            throw new SuspendedInstructorException("정지된 강사는 해당 작업을 수행할 수 없습니다.");
        }
    }

    private boolean hasInstructorAuthority(Authentication authentication) {
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (INSTRUCTOR_AUTHORITY.equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
