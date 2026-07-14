package com.lcs.lxp.course.model.entity;

import com.lcs.lxp.course.exception.CourseException;
import com.lcs.lxp.course.model.vo.ContentStatus;
import com.lcs.lxp.course.model.vo.CourseId;
import com.lcs.lxp.course.model.vo.InstructorId;
import com.lcs.lxp.course.model.vo.LectureId;
import com.lcs.lxp.course.model.vo.MissionId;
import com.lcs.lxp.course.model.vo.ReorderItem;
import com.lcs.lxp.course.model.vo.Sortable;
import com.lcs.lxp.course.model.vo.SortableType;
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
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

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

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column
    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContentStatus status;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Lecture> lectures = new ArrayList<>();

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Mission> missions = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column
    private OffsetDateTime updatedAt;

    @Column
    private OffsetDateTime deletedAt;

    private static final int MAX_DESCRIPTION_LENGTH = 4096;

    protected Course() {}

    public static Course create(InstructorId instructorId, Title title, String description, String thumbnailUrl) {
        if (instructorId == null) {
            throw new CourseException("강사 ID는 null일 수 없습니다.");
        }
        if (title == null) {
            throw new CourseException("제목은 null일 수 없습니다.");
        }
        if (description == null || description.isBlank()) {
            throw new CourseException("설명은 비어있을 수 없습니다.");
        }
        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new CourseException("설명은 4096자를 초과할 수 없습니다.");
        }
        Course course = new Course();
        course.instructorId = instructorId.value();
        course.title = title;
        course.description = description;
        course.thumbnailUrl = thumbnailUrl;
        course.status = ContentStatus.PRIVATE;
        course.createdAt = OffsetDateTime.now();
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

    public String getDescription() {
        return description;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    public List<Lecture> getLectures() {
        return lectures.stream()
                .sorted(Comparator.comparingInt(Lecture::getSortOrder))
                .toList();
    }

    public List<Mission> getMissions() {
        return missions.stream()
                .sorted(Comparator.comparingInt(Mission::getSortOrder))
                .toList();
    }

    /**
     * 강좌에 속한 강의와 미션을 하나의 목록으로 병합하여 sortOrder 오름차순으로 정렬한 통합 뷰를 반환한다.
     */
    public List<Sortable> getSortableItems() {
        List<Sortable> items = new ArrayList<>();
        items.addAll(lectures);
        items.addAll(missions);
        return items.stream()
                .sorted(Comparator.comparingInt(Sortable::getSortOrder))
                .toList();
    }

    public void update(Title newTitle, String description, String thumbnailUrl) {
        checkNotDeleted();
        if (status == ContentStatus.PUBLIC) {
            throw new CourseException("공개 상태에서는 강좌를 수정할 수 없습니다.");
        }
        if (newTitle == null) {
            throw new CourseException("제목은 null일 수 없습니다.");
        }
        if (description == null || description.isBlank()) {
            throw new CourseException("설명은 비어있을 수 없습니다.");
        }
        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new CourseException("설명은 4096자를 초과할 수 없습니다.");
        }
        this.title = newTitle;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.updatedAt = OffsetDateTime.now();
    }

    public Lecture addLecture(Title lectureTitle, String contentUrl, String contentType) {
        checkNotDeleted();
        if (status == ContentStatus.PUBLIC) {
            throw new CourseException("공개 상태에서는 강의를 추가할 수 없습니다.");
        }
        Lecture lecture = Lecture.create(this, lectureTitle, contentUrl, contentType, nextSortOrder());
        lectures.add(lecture);
        return lecture;
    }

    public Mission addMission(Title missionTitle, String content) {
        checkNotDeleted();
        if (status == ContentStatus.PUBLIC) {
            throw new CourseException("공개 상태에서는 미션을 추가할 수 없습니다.");
        }
        Mission mission = Mission.create(this, missionTitle, content, nextSortOrder());
        missions.add(mission);
        return mission;
    }

    /**
     * 강좌 내 강의/미션(soft delete로 삭제된 항목 포함)을 통틀어 사용 중인 최대 순번 다음 값을 반환한다.
     * soft delete된 항목도 순번을 계속 보유하므로, size 기반 계산이 아닌 실제 사용 중인 최댓값을
     * 기준으로 계산하여 신규 추가 시 기존 순번과 겹치지 않도록 한다.
     */
    private int nextSortOrder() {
        return currentMaxSortOrder() + 1;
    }

    private int currentMaxSortOrder() {
        return IntStream.concat(
                        lectures.stream().mapToInt(Lecture::getSortOrder),
                        missions.stream().mapToInt(Mission::getSortOrder))
                .max()
                .orElse(0);
    }

    /**
     * soft delete된(삭제된) 강의/미션만을 대상으로 사용 중인 최대 순번을 반환한다.
     * reorder()는 삭제되지 않은 항목 전체를 재배치 대상으로 삼으므로, 재배치로 새로 부여할
     * 순번의 기준(base)은 "재배치 대상이 아닌(즉 삭제된) 항목이 이미 사용 중인 최댓값"이어야
     * 재배치 후에도 삭제된 항목의 기존 순번과 절대 겹치지 않는다.
     */
    private int maxSortOrderAmongDeleted() {
        return IntStream.concat(
                        lectures.stream().filter(Lecture::isDeleted).mapToInt(Lecture::getSortOrder),
                        missions.stream().filter(Mission::isDeleted).mapToInt(Mission::getSortOrder))
                .max()
                .orElse(0);
    }

    public void updateLecture(LectureId lectureId, Title newTitle, String contentUrl, String contentType) {
        checkNotDeleted();
        findLecture(lectureId).update(newTitle, contentUrl, contentType);
    }

    public void publishLecture(LectureId lectureId) {
        checkNotDeleted();
        findLecture(lectureId).publish();
    }

    public void unpublishLecture(LectureId lectureId) {
        checkNotDeleted();
        findLecture(lectureId).unpublish();
    }

    public void deleteLecture(LectureId lectureId) {
        checkNotDeleted();
        findLecture(lectureId).delete();
    }

    public void updateMission(MissionId missionId, Title newTitle, String content) {
        checkNotDeleted();
        findMission(missionId).update(newTitle, content);
    }

    public void publishMission(MissionId missionId) {
        checkNotDeleted();
        findMission(missionId).publish();
    }

    public void unpublishMission(MissionId missionId) {
        checkNotDeleted();
        findMission(missionId).unpublish();
    }

    public void deleteMission(MissionId missionId) {
        checkNotDeleted();
        findMission(missionId).delete();
    }

    /**
     * 강좌에 속한 강의/미션의 노출 순서를 요청받은 순서(orderedItems)대로 재할당한다.
     * soft delete된(삭제된) 강의/미션은 사용자에게 노출되지 않으므로 재배치 대상 및
     * "전체 개수" 산정에서 제외한다. 요청 항목 개수는 삭제되지 않은 강의+미션 전체 개수와
     * 정확히 일치해야 하며(부분 재배치 불허), 중복된 항목이나 강좌에 속하지 않는 ID가
     * 포함된 경우 예외가 발생한다.
     * 새로 부여하는 순번은 삭제된 강의/미션이 이미 사용 중인 최댓값 다음부터 요청 순서대로
     * 순차 할당하여, 삭제된 항목의 기존 순번과 절대 겹치지 않도록 한다(연속성은 보장하지 않음).
     */
    public void reorder(List<ReorderItem> orderedItems) {
        checkNotDeleted();
        if (status == ContentStatus.PUBLIC) {
            throw new CourseException("공개 상태에서는 순서를 변경할 수 없습니다.");
        }
        List<Lecture> activeLectures = lectures.stream().filter(l -> !l.isDeleted()).toList();
        List<Mission> activeMissions = missions.stream().filter(m -> !m.isDeleted()).toList();
        int totalActive = activeLectures.size() + activeMissions.size();
        if (orderedItems == null || orderedItems.size() != totalActive) {
            throw new CourseException("순서 변경 대상 항목의 개수가 강좌에 속한 강의/미션 전체 개수와 일치하지 않습니다.");
        }
        Set<ReorderItem> uniqueItems = new HashSet<>(orderedItems);
        if (uniqueItems.size() != orderedItems.size()) {
            throw new CourseException("순서 변경 대상 목록에 중복된 항목이 있습니다.");
        }
        int order = maxSortOrderAmongDeleted();
        for (ReorderItem item : orderedItems) {
            order++;
            if (item.type() == SortableType.LECTURE) {
                findActiveLecture(activeLectures, item.id()).assignSortOrder(order);
            } else {
                findActiveMission(activeMissions, item.id()).assignSortOrder(order);
            }
        }
    }

    private Lecture findActiveLecture(List<Lecture> activeLectures, Long lectureId) {
        return activeLectures.stream()
                .filter(l -> lectureId.equals(l.getRawId()))
                .findFirst()
                .orElseThrow(() -> new CourseException("강의를 찾을 수 없습니다."));
    }

    private Mission findActiveMission(List<Mission> activeMissions, Long missionId) {
        return activeMissions.stream()
                .filter(m -> missionId.equals(m.getRawId()))
                .findFirst()
                .orElseThrow(() -> new CourseException("미션을 찾을 수 없습니다."));
    }

    private Lecture findLecture(LectureId lectureId) {
        return lectures.stream()
                .filter(l -> lectureId.value().equals(l.getRawId()))
                .findFirst()
                .orElseThrow(() -> new CourseException("강의를 찾을 수 없습니다."));
    }

    private Mission findMission(MissionId missionId) {
        return missions.stream()
                .filter(m -> missionId.value().equals(m.getRawId()))
                .findFirst()
                .orElseThrow(() -> new CourseException("미션을 찾을 수 없습니다."));
    }

    public void publish() {
        checkNotDeleted();
        if (lectures.isEmpty() || missions.isEmpty()) {
            throw new CourseException("강의와 미션을 1개 이상 포함해야 공개할 수 있습니다.");
        }
        this.status = ContentStatus.PUBLIC;
        this.updatedAt = OffsetDateTime.now();
    }

    public void unpublish() {
        checkNotDeleted();
        this.status = ContentStatus.PRIVATE;
        this.updatedAt = OffsetDateTime.now();
    }

    public void delete() {
        checkNotDeleted();
        this.deletedAt = OffsetDateTime.now();
    }

    private void checkNotDeleted() {
        if (deletedAt != null) {
            throw new CourseException("삭제된 강좌는 수정할 수 없습니다.");
        }
    }
}
