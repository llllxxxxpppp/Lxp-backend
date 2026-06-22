package com.lcs.lxp.course.model.entity;

import com.lcs.lxp.course.exception.CourseException;
import com.lcs.lxp.course.model.vo.ContentStatus;
import com.lcs.lxp.course.model.vo.CourseId;
import com.lcs.lxp.course.model.vo.InstructorId;
import com.lcs.lxp.course.model.vo.Title;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "instructor_id", nullable = false)
    private Long instructorId;

    @Embedded
    private Title title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContentStatus status;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Lecture> lectures = new ArrayList<>();

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Mission> missions = new ArrayList<>();

    @Column(nullable = false)
    private boolean deleted;

    protected Course() {}

    public static Course create(InstructorId instructorId, Title title) {
        Course course = new Course();
        course.instructorId = instructorId.value();
        course.title = title;
        course.status = ContentStatus.PRIVATE;
        return course;
    }

    public CourseId getId() {
        return new CourseId(id);
    }

    public InstructorId getInstructorId() {
        return new InstructorId(instructorId);
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

    public List<Lecture> getLectures() {
        return List.copyOf(lectures);
    }

    public List<Mission> getMissions() {
        return List.copyOf(missions);
    }

    public void update(Title newTitle) {
        if (status == ContentStatus.PUBLIC) {
            throw new CourseException("공개 상태에서는 강좌를 수정할 수 없습니다.");
        }
        this.title = newTitle;
    }

    public Lecture addLecture(Title lectureTitle) {
        if (status == ContentStatus.PUBLIC) {
            throw new CourseException("공개 상태에서는 강의를 추가할 수 없습니다.");
        }
        Lecture lecture = Lecture.create(this, lectureTitle);
        lectures.add(lecture);
        return lecture;
    }

    public Mission addMission(Title missionTitle) {
        if (status == ContentStatus.PUBLIC) {
            throw new CourseException("공개 상태에서는 미션을 추가할 수 없습니다.");
        }
        Mission mission = Mission.create(this, missionTitle);
        missions.add(mission);
        return mission;
    }

    public void publish() {
        boolean hasActiveLecture = lectures.stream().anyMatch(l -> !l.isDeleted());
        boolean hasActiveMission = missions.stream().anyMatch(m -> !m.isDeleted());
        if (!hasActiveLecture || !hasActiveMission) {
            throw new CourseException("강의와 미션을 1개 이상 포함해야 공개할 수 있습니다.");
        }
        this.status = ContentStatus.PUBLIC;
    }

    public void unpublish() {
        this.status = ContentStatus.PRIVATE;
        this.deleted = true;
    }
}
