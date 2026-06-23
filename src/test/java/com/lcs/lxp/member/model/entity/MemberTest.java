package com.lcs.lxp.member.model.entity;

import com.lcs.lxp.member.exception.MemberException;
import com.lcs.lxp.member.model.MemberRole;
import com.lcs.lxp.member.model.vo.InstructorProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemberTest {

    // -------------------------------------------------------------------------
    // createMember
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("유효한 이메일과 패스워드로 일반 회원이 생성된다")
    void givenValidEmailAndPassword_whenCreateMember_thenRoleIsMember() {
        Member member = Member.createMember("user@example.com", "encoded_password");
        assertEquals(MemberRole.MEMBER, member.getRole());
    }

    @Test
    @DisplayName("일반 회원 생성 시 수정 일자는 null이다")
    void givenValidValues_whenCreateMember_thenUpdatedAtIsNull() {
        Member member = Member.createMember("user@example.com", "encoded_password");
        assertNull(member.getUpdatedAt());
    }

    @Test
    @DisplayName("일반 회원 생성 시 탈퇴 상태는 false이다")
    void givenValidValues_whenCreateMember_thenDeletedIsFalse() {
        Member member = Member.createMember("user@example.com", "encoded_password");
        assertFalse(member.isDeleted());
    }

    @Test
    @DisplayName("이메일이 null이면 일반 회원 생성 시 예외가 발생한다")
    void givenNullEmail_whenCreateMember_thenThrowsException() {
        assertThrows(MemberException.class, () -> Member.createMember(null, "encoded_password"));
    }

    @Test
    @DisplayName("이메일이 빈 문자열이면 일반 회원 생성 시 예외가 발생한다")
    void givenBlankEmail_whenCreateMember_thenThrowsException() {
        assertThrows(MemberException.class, () -> Member.createMember("   ", "encoded_password"));
    }

    @Test
    @DisplayName("이메일 형식이 유효하지 않으면 일반 회원 생성 시 예외가 발생한다")
    void givenInvalidEmail_whenCreateMember_thenThrowsException() {
        assertThrows(MemberException.class, () -> Member.createMember("not-an-email", "encoded_password"));
    }

    @Test
    @DisplayName("패스워드가 null이면 일반 회원 생성 시 예외가 발생한다")
    void givenNullPassword_whenCreateMember_thenThrowsException() {
        assertThrows(MemberException.class, () -> Member.createMember("user@example.com", null));
    }

    @Test
    @DisplayName("패스워드가 빈 문자열이면 일반 회원 생성 시 예외가 발생한다")
    void givenBlankPassword_whenCreateMember_thenThrowsException() {
        assertThrows(MemberException.class, () -> Member.createMember("user@example.com", "   "));
    }

    // -------------------------------------------------------------------------
    // createInstructor
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("유효한 값으로 강사가 생성된다")
    void givenValidValues_whenCreateInstructor_thenRoleIsInstructor() {
        Member instructor = Member.createInstructor("instructor@example.com", "encoded_password",
                "홍길동", null, null);
        assertEquals(MemberRole.INSTRUCTOR, instructor.getRole());
    }

    @Test
    @DisplayName("강사 생성 시 프로필 정보가 설정된다")
    void givenValidValues_whenCreateInstructor_thenProfileIsSet() {
        Member instructor = Member.createInstructor("instructor@example.com", "encoded_password",
                "홍길동", "/images/profile.jpg", "안녕하세요");
        InstructorProfile profile = instructor.getProfile();
        assertNotNull(profile);
        assertEquals("홍길동", profile.getName());
    }

    // -------------------------------------------------------------------------
    // createAdmin
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("유효한 값으로 어드민이 생성된다")
    void givenValidValues_whenCreateAdmin_thenRoleIsAdmin() {
        Member admin = Member.createAdmin("admin@example.com", "encoded_password");
        assertEquals(MemberRole.ADMIN, admin.getRole());
    }

    // -------------------------------------------------------------------------
    // updateEmail
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("유효한 이메일로 이메일을 변경할 수 있다")
    void givenValidEmail_whenUpdateEmail_thenEmailIsUpdated() {
        Member member = Member.createMember("user@example.com", "encoded_password");
        member.updateEmail("new@example.com");
        assertEquals("new@example.com", member.getEmail());
    }

    @Test
    @DisplayName("이메일 변경 후 수정 일자가 갱신된다")
    void givenValidEmail_whenUpdateEmail_thenUpdatedAtIsSet() {
        Member member = Member.createMember("user@example.com", "encoded_password");
        member.updateEmail("new@example.com");
        assertNotNull(member.getUpdatedAt());
    }

    @Test
    @DisplayName("유효하지 않은 이메일로 변경하면 예외가 발생한다")
    void givenInvalidEmail_whenUpdateEmail_thenThrowsException() {
        Member member = Member.createMember("user@example.com", "encoded_password");
        assertThrows(MemberException.class, () -> member.updateEmail("invalid-email"));
    }

    // -------------------------------------------------------------------------
    // updatePassword
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("유효한 패스워드로 패스워드를 변경할 수 있다")
    void givenValidPassword_whenUpdatePassword_thenPasswordIsUpdated() {
        Member member = Member.createMember("user@example.com", "old_password");
        member.updatePassword("new_password");
        assertEquals("new_password", member.getPassword());
    }

    @Test
    @DisplayName("패스워드 변경 후 수정 일자가 갱신된다")
    void givenValidPassword_whenUpdatePassword_thenUpdatedAtIsSet() {
        Member member = Member.createMember("user@example.com", "old_password");
        member.updatePassword("new_password");
        assertNotNull(member.getUpdatedAt());
    }

    @Test
    @DisplayName("패스워드가 빈 문자열이면 패스워드 변경 시 예외가 발생한다")
    void givenBlankPassword_whenUpdatePassword_thenThrowsException() {
        Member member = Member.createMember("user@example.com", "encoded_password");
        assertThrows(MemberException.class, () -> member.updatePassword("   "));
    }

    // -------------------------------------------------------------------------
    // updateProfile
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("강사는 프로필을 수정할 수 있다")
    void givenInstructor_whenUpdateProfile_thenProfileIsUpdated() {
        Member instructor = Member.createInstructor("instructor@example.com", "encoded_password",
                "홍길동", null, null);
        assertDoesNotThrow(() -> instructor.updateProfile("김철수", "/new_image.jpg", "새 소개"));
        assertEquals("김철수", instructor.getProfile().getName());
    }

    @Test
    @DisplayName("프로필 수정 후 수정 일자가 갱신된다")
    void givenInstructor_whenUpdateProfile_thenUpdatedAtIsSet() {
        Member instructor = Member.createInstructor("instructor@example.com", "encoded_password",
                "홍길동", null, null);
        instructor.updateProfile("김철수", null, null);
        assertNotNull(instructor.getUpdatedAt());
    }

    @Test
    @DisplayName("강사가 아닌 회원이 프로필을 수정하면 예외가 발생한다")
    void givenMember_whenUpdateProfile_thenThrowsException() {
        Member member = Member.createMember("user@example.com", "encoded_password");
        assertThrows(MemberException.class, () -> member.updateProfile("홍길동", null, null));
    }

    @Test
    @DisplayName("프로필 이름이 빈 문자열이면 프로필 수정 시 예외가 발생한다")
    void givenBlankProfileName_whenUpdateProfile_thenThrowsException() {
        Member instructor = Member.createInstructor("instructor@example.com", "encoded_password",
                "홍길동", null, null);
        assertThrows(MemberException.class, () -> instructor.updateProfile("", null, null));
    }

    // -------------------------------------------------------------------------
    // withdraw
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("회원 탈퇴 처리 시 deleted가 true가 된다")
    void givenMember_whenWithdraw_thenDeletedIsTrue() {
        Member member = Member.createMember("user@example.com", "encoded_password");
        member.withdraw();
        assertTrue(member.isDeleted());
    }

    @Test
    @DisplayName("회원 탈퇴 처리 후 수정 일자가 갱신된다")
    void givenMember_whenWithdraw_thenUpdatedAtIsSet() {
        Member member = Member.createMember("user@example.com", "encoded_password");
        member.withdraw();
        assertNotNull(member.getUpdatedAt());
    }

    // -------------------------------------------------------------------------
    // suspend
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("강사 정지 처리 시 deleted가 true가 된다")
    void givenInstructor_whenSuspend_thenDeletedIsTrue() {
        Member instructor = Member.createInstructor("instructor@example.com", "encoded_password",
                "홍길동", null, null);
        instructor.suspend();
        assertTrue(instructor.isDeleted());
    }

    @Test
    @DisplayName("강사 정지 처리 후 수정 일자가 갱신된다")
    void givenInstructor_whenSuspend_thenUpdatedAtIsSet() {
        Member instructor = Member.createInstructor("instructor@example.com", "encoded_password",
                "홍길동", null, null);
        instructor.suspend();
        assertNotNull(instructor.getUpdatedAt());
    }
}
