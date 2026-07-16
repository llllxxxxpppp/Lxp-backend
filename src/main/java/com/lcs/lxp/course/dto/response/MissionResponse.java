package com.lcs.lxp.course.dto.response;

import com.lcs.lxp.course.model.entity.Mission;

public record MissionResponse(Long missionId, String title, String status, int sortOrder) {

    public static MissionResponse from(Mission mission) {
        return new MissionResponse(
                mission.getId().value(),
                mission.getTitle().getValue(),
                mission.getStatus().name(),
                mission.getSortOrder());
    }
}
