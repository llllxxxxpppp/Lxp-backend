package com.lcs.lxp.course.repository;

import com.lcs.lxp.course.model.Lecture;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LectureRepository extends JpaRepository<Lecture, Long> {
}
