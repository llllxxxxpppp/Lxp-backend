package com.lcs.lxp.course.event;

import com.lcs.lxp.course.service.CourseService;
import com.lcs.lxp.member.event.InstructorSuspendedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Member BC가 발행하는 {@link InstructorSuspendedEvent}를 구독해
 * 해당 강사가 소유한 공개 강좌를 전부 비공개 처리한다.
 *
 * 강사 정지 트랜잭션(Member BC)이 커밋된 이후에 안전하게 강좌를 조회/수정해야 하므로
 * {@link TransactionalEventListener}의 {@code AFTER_COMMIT} 단계에서 처리한다.
 */
@Component
public class InstructorSuspendedEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstructorSuspendedEventListener.class);

    private final CourseService courseService;

    public InstructorSuspendedEventListener(CourseService courseService) {
        this.courseService = courseService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(InstructorSuspendedEvent event) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(
                    "Handling InstructorSuspendedEvent started: eventId={}, occurredAt={}, instructorId={}",
                    event.getEventId(), event.getOccurredAt(), event.getInstructorId());
        }

        courseService.unpublishAllByInstructor(event.getInstructorId());

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(
                    "Handling InstructorSuspendedEvent finished: eventId={}, occurredAt={}, instructorId={}",
                    event.getEventId(), event.getOccurredAt(), event.getInstructorId());
        }
    }
}
