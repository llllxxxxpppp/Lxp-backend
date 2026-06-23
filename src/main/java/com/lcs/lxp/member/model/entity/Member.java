package com.lcs.lxp.member.model.entity;

import com.lcs.lxp.member.exception.MemberException;
import com.lcs.lxp.member.model.MemberRole;
import com.lcs.lxp.member.model.vo.InstructorProfile;
import com.lcs.lxp.member.model.vo.MemberId;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.regex.Pattern;

@Entity
@Table(name = "members")
public class Member {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w._%+\\-]+@[\\w.\\-]+\\.[A-Za-z]{2,}$");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberRole role;

    @Embedded
    private InstructorProfile profile;

    @Column(nullable = false)
    private boolean deleted;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column
    private OffsetDateTime updatedAt;

    protected Member() {}

    public static Member createMember(String email, String encodedPassword) {
        validateEmail(email);
        validatePassword(encodedPassword);
        Member member = new Member();
        member.email = email;
        member.password = encodedPassword;
        member.role = MemberRole.MEMBER;
        member.deleted = false;
        member.createdAt = OffsetDateTime.now();
        return member;
    }

    public static Member createInstructor(String email, String encodedPassword,
            String name, String profileImageUrl, String introduction) {
        validateEmail(email);
        validatePassword(encodedPassword);
        Member member = new Member();
        member.email = email;
        member.password = encodedPassword;
        member.role = MemberRole.INSTRUCTOR;
        member.profile = InstructorProfile.of(name, profileImageUrl, introduction);
        member.deleted = false;
        member.createdAt = OffsetDateTime.now();
        return member;
    }

    public static Member createAdmin(String email, String encodedPassword) {
        validateEmail(email);
        validatePassword(encodedPassword);
        Member member = new Member();
        member.email = email;
        member.password = encodedPassword;
        member.role = MemberRole.ADMIN;
        member.deleted = false;
        member.createdAt = OffsetDateTime.now();
        return member;
    }

    public MemberId getId() {
        return new MemberId(id);
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public MemberRole getRole() {
        return role;
    }

    public InstructorProfile getProfile() {
        return profile;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void updateEmail(String email) {
        validateEmail(email);
        this.email = email;
        this.updatedAt = OffsetDateTime.now();
    }

    public void updatePassword(String encodedPassword) {
        validatePassword(encodedPassword);
        this.password = encodedPassword;
        this.updatedAt = OffsetDateTime.now();
    }

    public void updateProfile(String name, String profileImageUrl, String introduction) {
        if (role != MemberRole.INSTRUCTOR) {
            throw new MemberException("강사만 프로필을 수정할 수 있습니다.");
        }
        this.profile = InstructorProfile.of(name, profileImageUrl, introduction);
        this.updatedAt = OffsetDateTime.now();
    }

    public void withdraw() {
        this.deleted = true;
        this.updatedAt = OffsetDateTime.now();
    }

    public void suspend() {
        this.deleted = true;
        this.updatedAt = OffsetDateTime.now();
    }

    private static void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new MemberException("이메일은 비어있을 수 없습니다.");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new MemberException("유효하지 않은 이메일 형식입니다.");
        }
    }

    private static void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new MemberException("패스워드는 비어있을 수 없습니다.");
        }
    }
}
