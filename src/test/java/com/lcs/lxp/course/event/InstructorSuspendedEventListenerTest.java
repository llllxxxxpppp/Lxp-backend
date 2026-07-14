package com.lcs.lxp.course.event;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.lcs.lxp.course.service.CourseService;
import com.lcs.lxp.member.event.InstructorSuspendedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * COURSE-08a: Member BC가 발행하는 InstructorSuspendedEvent를 Course BC가 구독해
 * 해당 강사의 공개 강좌를 전부 비공개 처리하는지 검증한다.
 *
 * 로그 레벨/내용 검증은 프로젝트 관례(로그 캡처 인프라 부재)에 따라 생략하고,
 * 이벤트 수신 시 서비스로의 위임(instructorId 전달)에 집중한다.
 */
@ExtendWith(MockitoExtension.class)
class InstructorSuspendedEventListenerTest {

    @Mock
    private CourseService courseService;

    @InjectMocks
    private InstructorSuspendedEventListener instructorSuspendedEventListener;

    @Test
    @DisplayName("강사 정지 이벤트를 수신하면 해당 강사의 공개 강좌 전체를 비공개 처리한다")
    void givenInstructorSuspendedEvent_whenHandle_thenUnpublishesAllCoursesOfThatInstructor() {
        InstructorSuspendedEvent event = new InstructorSuspendedEvent(1L);

        instructorSuspendedEventListener.handle(event);

        verify(courseService).unpublishAllByInstructor(1L);
    }

    @Test
    @DisplayName("다른 강사 ID를 담은 이벤트를 수신하면 그 강사 ID로만 대량 비공개 처리가 호출된다")
    void givenEventForDifferentInstructor_whenHandle_thenServiceIsCalledWithThatInstructorIdOnly() {
        InstructorSuspendedEvent event = new InstructorSuspendedEvent(42L);

        instructorSuspendedEventListener.handle(event);

        verify(courseService).unpublishAllByInstructor(42L);
        verifyNoMoreInteractions(courseService);
    }
}
