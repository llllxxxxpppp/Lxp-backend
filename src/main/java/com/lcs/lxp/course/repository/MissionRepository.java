package com.lcs.lxp.course.repository;

import com.lcs.lxp.course.model.entity.Mission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MissionRepository extends JpaRepository<Mission, Long> {
}
