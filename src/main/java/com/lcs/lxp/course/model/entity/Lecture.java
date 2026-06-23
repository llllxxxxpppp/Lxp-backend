package com.lcs.lxp.course.model.entity;

import com.lcs.lxp.course.exception.CourseException;
import com.lcs.lxp.course.model.vo.ContentStatus;
import com.lcs.lxp.course.model.vo.LectureId;
import com.lcs.lxp.course.model.vo.Title;
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
import java.time.OffsetDateTime;

@Entity
@Table(name = "lectures")
public class Lecture {

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
    private boolean deleted;

    @Column
    private String contentUrl;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column
    private OffsetDateTime updatedAt;

    protected Lecture() {}

    static Lecture create(Course course, Title title) {
        Lecture lecture = new Lecture();
        lecture.course = course;
        lecture.title = title;
        lecture.status = ContentStatus.PRIVATE;
        lecture.createdAt = OffsetDateTime.now();
        return lecture;
    }

    public LectureId getId() {
        return new LectureId(id);
    }

    Long getRawId() {
        return id;
    }

    public ContentStatus getStatus() {
        return status;
    }

    public Title getTitle() {
        return title;
    }

    public String getContentUrl() {
        return contentUrl;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    void update(Title newTitle, String contentUrl) {
        if (course.getStatus() == ContentStatus.PUBLIC && status == ContentStatus.PUBLIC) {
            throw new CourseException("공개 상태에서는 강의를 수정할 수 없습니다.");
        }
        if (contentUrl == null || contentUrl.isBlank()) {
            throw new CourseException("강의 자료 URL은 비어있을 수 없습니다.");
        }
        this.title = newTitle;
        this.contentUrl = contentUrl;
        this.updatedAt = OffsetDateTime.now();
    }

    void publish() {
        this.status = ContentStatus.PUBLIC;
        this.updatedAt = OffsetDateTime.now();
    }

    void unpublish() {
        this.status = ContentStatus.PRIVATE;
        this.deleted = true;
        this.updatedAt = OffsetDateTime.now();
    }
}
