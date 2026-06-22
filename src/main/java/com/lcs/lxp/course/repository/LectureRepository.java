package com.lcs.lxp.course.repository;

import com.lcs.lxp.course.model.entity.Lecture;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LectureRepository extends JpaRepository<Lecture, Long> {
}
