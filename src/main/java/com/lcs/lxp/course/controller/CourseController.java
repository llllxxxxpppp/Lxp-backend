package com.lcs.lxp.course.controller;

import com.lcs.lxp.course.dto.request.AddLectureRequest;
import com.lcs.lxp.course.dto.request.AddMissionRequest;
import com.lcs.lxp.course.dto.request.CreateCourseRequest;
import com.lcs.lxp.course.dto.request.ReorderRequest;
import com.lcs.lxp.course.dto.request.UpdateCourseRequest;
import com.lcs.lxp.course.dto.response.CourseDetailResponse;
import com.lcs.lxp.course.dto.response.CoursePageResponse;
import com.lcs.lxp.course.dto.response.CourseSummaryResponse;
import com.lcs.lxp.course.exception.CourseException;
import com.lcs.lxp.course.service.CourseService;
import com.lcs.lxp.security.principal.CustomUserPrincipal;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private static final String ADMIN_AUTHORITY = "ROLE_ADMIN";

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public ResponseEntity<CoursePageResponse> getCourses(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(courseService.getCourses(keyword, page, size));
    }

    @GetMapping("/{courseId}")
    public ResponseEntity<CourseSummaryResponse> getCourseSummary(@PathVariable Long courseId) {
        return ResponseEntity.ok(courseService.getCourseSummary(courseId));
    }

    @GetMapping("/{courseId}/detail")
    public ResponseEntity<CourseDetailResponse> getCourseDetail(@PathVariable Long courseId) {
        return ResponseEntity.ok(courseService.getCourseDetail(courseId));
    }

    @PostMapping
    public ResponseEntity<Void> createCourse(
            @RequestBody @Valid CreateCourseRequest request,
            Authentication authentication) {
        CustomUserPrincipal principal = resolvePrincipal(authentication);
        Long instructorId = principal.getUserId();
        courseService.createCourse(instructorId, request.title(), request.description(), request.thumbnailUrl());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PatchMapping("/{courseId}")
    public ResponseEntity<Void> updateCourse(
            @PathVariable Long courseId,
            @RequestBody @Valid UpdateCourseRequest request,
            Authentication authentication) {
        CustomUserPrincipal principal = resolvePrincipal(authentication);
        courseService.updateCourse(
                courseId, request.title(), request.description(), request.thumbnailUrl(),
                principal.getUserId(), isAdmin(principal));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{courseId}/publish")
    public ResponseEntity<Void> publishCourse(@PathVariable Long courseId, Authentication authentication) {
        CustomUserPrincipal principal = resolvePrincipal(authentication);
        courseService.publishCourse(courseId, principal.getUserId(), isAdmin(principal));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{courseId}/unpublish")
    public ResponseEntity<Void> unpublishCourse(@PathVariable Long courseId, Authentication authentication) {
        CustomUserPrincipal principal = resolvePrincipal(authentication);
        courseService.unpublishCourse(courseId, principal.getUserId(), isAdmin(principal));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long courseId, Authentication authentication) {
        CustomUserPrincipal principal = resolvePrincipal(authentication);
        courseService.deleteCourse(courseId, principal.getUserId(), isAdmin(principal));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{courseId}/lectures")
    public ResponseEntity<Void> addLecture(
            @PathVariable Long courseId,
            @RequestBody @Valid AddLectureRequest request) {
        courseService.addLecture(courseId, request.title(), request.contentUrl(), request.contentType());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/{courseId}/lectures/{lectureId}/publish")
    public ResponseEntity<Void> publishLecture(
            @PathVariable Long courseId,
            @PathVariable Long lectureId,
            Authentication authentication) {
        CustomUserPrincipal principal = resolvePrincipal(authentication);
        courseService.publishLecture(courseId, lectureId, principal.getUserId(), isAdmin(principal));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{courseId}/lectures/{lectureId}/unpublish")
    public ResponseEntity<Void> unpublishLecture(
            @PathVariable Long courseId,
            @PathVariable Long lectureId,
            Authentication authentication) {
        CustomUserPrincipal principal = resolvePrincipal(authentication);
        courseService.unpublishLecture(courseId, lectureId, principal.getUserId(), isAdmin(principal));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{courseId}/lectures/{lectureId}")
    public ResponseEntity<Void> deleteLecture(
            @PathVariable Long courseId,
            @PathVariable Long lectureId,
            Authentication authentication) {
        CustomUserPrincipal principal = resolvePrincipal(authentication);
        courseService.deleteLecture(courseId, lectureId, principal.getUserId(), isAdmin(principal));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{courseId}/missions")
    public ResponseEntity<Void> addMission(
            @PathVariable Long courseId,
            @RequestBody @Valid AddMissionRequest request) {
        courseService.addMission(courseId, request.title(), request.content());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/{courseId}/missions/{missionId}/publish")
    public ResponseEntity<Void> publishMission(
            @PathVariable Long courseId,
            @PathVariable Long missionId,
            Authentication authentication) {
        CustomUserPrincipal principal = resolvePrincipal(authentication);
        courseService.publishMission(courseId, missionId, principal.getUserId(), isAdmin(principal));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{courseId}/missions/{missionId}/unpublish")
    public ResponseEntity<Void> unpublishMission(
            @PathVariable Long courseId,
            @PathVariable Long missionId,
            Authentication authentication) {
        CustomUserPrincipal principal = resolvePrincipal(authentication);
        courseService.unpublishMission(courseId, missionId, principal.getUserId(), isAdmin(principal));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{courseId}/missions/{missionId}")
    public ResponseEntity<Void> deleteMission(
            @PathVariable Long courseId,
            @PathVariable Long missionId,
            Authentication authentication) {
        CustomUserPrincipal principal = resolvePrincipal(authentication);
        courseService.deleteMission(courseId, missionId, principal.getUserId(), isAdmin(principal));
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{courseId}/reorder")
    public ResponseEntity<Void> reorderItems(
            @PathVariable Long courseId,
            @RequestBody @Valid ReorderRequest request) {
        List<String> itemTypes = request.items().stream()
                .map(item -> item.type().name())
                .toList();
        List<Long> itemIds = request.items().stream()
                .map(ReorderRequest.Item::id)
                .toList();
        courseService.reorderItems(courseId, itemTypes, itemIds);
        return ResponseEntity.ok().build();
    }

    private CustomUserPrincipal resolvePrincipal(Authentication authentication) {
        if (authentication.getPrincipal() instanceof CustomUserPrincipal principal) {
            return principal;
        }
        throw new CourseException("인증 정보가 올바르지 않습니다.");
    }

    private boolean isAdmin(CustomUserPrincipal principal) {
        for (GrantedAuthority authority : principal.getAuthorities()) {
            if (ADMIN_AUTHORITY.equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
