package com.lcs.lxp.course.model;

import com.lcs.lxp.course.exception.CourseException;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "missions")
public class Mission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Embedded
    private Title title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContentStatus status;

    @Column(nullable = false)
    private boolean deleted = false;

    protected Mission() {}

    static Mission create(Course course, Title title) {
        Mission mission = new Mission();
        mission.course = course;
        mission.title = title;
        mission.status = ContentStatus.PRIVATE;
        return mission;
    }

    public MissionId getId() {
        return new MissionId(id);
    }

    public ContentStatus getStatus() {
        return status;
    }

    public Title getTitle() {
        return title;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void update(Title newTitle) {
        if (course.getStatus() == ContentStatus.PUBLIC && status == ContentStatus.PUBLIC) {
            throw new CourseException("공개 상태에서는 미션을 수정할 수 없습니다.");
        }
        this.title = newTitle;
    }

    public void publish() {
        this.status = ContentStatus.PUBLIC;
    }

    public void unpublish() {
        this.status = ContentStatus.PRIVATE;
        this.deleted = true;
    }
}
