package com.lcs.lxp.course.controller;

import com.lcs.lxp.course.dto.request.AddLectureRequest;
import com.lcs.lxp.course.dto.request.AddMissionRequest;
import com.lcs.lxp.course.dto.request.CreateCourseRequest;
import com.lcs.lxp.course.dto.request.UpdateCourseRequest;
import com.lcs.lxp.course.dto.response.CourseDetailResponse;
import com.lcs.lxp.course.dto.response.CourseSummaryResponse;
import com.lcs.lxp.course.service.CourseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
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
        Long instructorId = Long.parseLong(authentication.getName());
        courseService.createCourse(instructorId, request.title(), request.description(), request.thumbnailUrl());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PatchMapping("/{courseId}")
    public ResponseEntity<Void> updateCourse(
            @PathVariable Long courseId,
            @RequestBody @Valid UpdateCourseRequest request) {
        courseService.updateCourse(courseId, request.title(), request.description(), request.thumbnailUrl());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{courseId}/publish")
    public ResponseEntity<Void> publishCourse(@PathVariable Long courseId) {
        courseService.publishCourse(courseId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{courseId}/unpublish")
    public ResponseEntity<Void> unpublishCourse(@PathVariable Long courseId) {
        courseService.unpublishCourse(courseId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{courseId}/lectures")
    public ResponseEntity<Void> addLecture(
            @PathVariable Long courseId,
            @RequestBody @Valid AddLectureRequest request) {
        courseService.addLecture(courseId, request.title(), request.contentUrl());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/{courseId}/lectures/{lectureId}/publish")
    public ResponseEntity<Void> publishLecture(
            @PathVariable Long courseId,
            @PathVariable Long lectureId) {
        courseService.publishLecture(courseId, lectureId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{courseId}/lectures/{lectureId}/unpublish")
    public ResponseEntity<Void> unpublishLecture(
            @PathVariable Long courseId,
            @PathVariable Long lectureId) {
        courseService.unpublishLecture(courseId, lectureId);
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
            @PathVariable Long missionId) {
        courseService.publishMission(courseId, missionId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{courseId}/missions/{missionId}/unpublish")
    public ResponseEntity<Void> unpublishMission(
            @PathVariable Long courseId,
            @PathVariable Long missionId) {
        courseService.unpublishMission(courseId, missionId);
        return ResponseEntity.ok().build();
    }
}
