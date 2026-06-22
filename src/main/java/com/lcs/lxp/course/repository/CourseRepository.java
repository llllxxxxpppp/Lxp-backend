package com.lcs.lxp.course.repository;

import com.lcs.lxp.course.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {
}
