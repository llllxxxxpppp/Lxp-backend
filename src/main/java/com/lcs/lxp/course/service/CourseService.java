package com.lcs.lxp.course.service;

import com.lcs.lxp.course.dto.response.CourseDetailResponse;
import com.lcs.lxp.course.dto.response.CourseSummaryResponse;
import com.lcs.lxp.course.exception.CourseException;
import com.lcs.lxp.course.model.entity.Course;
import com.lcs.lxp.course.model.vo.InstructorId;
import com.lcs.lxp.course.model.vo.Title;
import com.lcs.lxp.course.repository.CourseRepository;
import com.lcs.lxp.member.model.MemberRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CourseService {

    private final CourseRepository courseRepository;

    public CourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    @Transactional(readOnly = true)
    public CourseSummaryResponse getCourseSummary(Long courseId) {
        return CourseSummaryResponse.from(getCourse(courseId));
    }

    @Transactional(readOnly = true)
    public CourseDetailResponse getCourseDetail(Long courseId) {
        return CourseDetailResponse.from(getCourse(courseId));
    }

    public void createCourse(Long instructorId, String title) {
        requireRole(MemberRole.INSTRUCTOR);
        Course course = Course.create(new InstructorId(instructorId), new Title(title));
        courseRepository.save(course);
    }

    public void updateCourse(Long courseId, String newTitle) {
        Course course = getCourse(courseId);
        course.update(new Title(newTitle));
    }

    public void publishCourse(Long courseId) {
        requireRole(MemberRole.INSTRUCTOR);
        Course course = getCourse(courseId);
        course.publish();
    }

    public void unpublishCourse(Long courseId) {
        requireRole(MemberRole.INSTRUCTOR, MemberRole.ADMIN);
        Course course = getCourse(courseId);
        course.unpublish();
    }

    private Course getCourse(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseException("강좌를 찾을 수 없습니다."));
    }

    private void requireRole(MemberRole... allowedRoles) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new CourseException("인증된 사용자만 접근할 수 있습니다.");
        }
        for (MemberRole role : allowedRoles) {
            if (auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_" + role.name()))) {
                return;
            }
        }
        throw new CourseException("접근 권한이 없습니다.");
    }
}
