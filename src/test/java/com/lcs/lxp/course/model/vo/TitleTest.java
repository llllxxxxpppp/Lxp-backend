package com.lcs.lxp.course.model.vo;

import com.lcs.lxp.course.exception.CourseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TitleTest {

    @Test
    @DisplayName("유효한 제목으로 생성이 된다")
    void givenValidValue_whenCreateTitle_thenSucceeds() {
        assertDoesNotThrow(() -> new Title("유효한 제목"));
    }

    @Test
    @DisplayName("제목이 정확히 100자이면 생성이 된다")
    void givenValueOfExactMaxLength_whenCreateTitle_thenSucceeds() {
        String value = "a".repeat(Title.MAX_LENGTH);
        assertDoesNotThrow(() -> new Title(value));
    }

    @Test
    @DisplayName("null 제목으로 생성하면 예외가 발생한다")
    void givenNullValue_whenCreateTitle_thenThrowsException() {
        assertThrows(CourseException.class, () -> new Title(null));
    }

    @Test
    @DisplayName("빈 제목으로 생성하면 예외가 발생한다")
    void givenEmptyValue_whenCreateTitle_thenThrowsException() {
        assertThrows(CourseException.class, () -> new Title(""));
    }

    @Test
    @DisplayName("공백 제목으로 생성하면 예외가 발생한다")
    void givenBlankValue_whenCreateTitle_thenThrowsException() {
        assertThrows(CourseException.class, () -> new Title("   "));
    }

    @Test
    @DisplayName("제목이 100자를 초과하면 예외가 발생한다")
    void givenValueExceedingMaxLength_whenCreateTitle_thenThrowsException() {
        String value = "a".repeat(Title.MAX_LENGTH + 1);
        assertThrows(CourseException.class, () -> new Title(value));
    }
}
