package com.focusfortress.unit.mapper;

import com.focusfortress.dto.HabitProgressDTO;
import com.focusfortress.mapper.HabitProgressMapper;
import com.focusfortress.model.Habit;
import com.focusfortress.model.HabitProgress;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import static com.focusfortress.unit.TestData.habit;
import static com.focusfortress.unit.TestData.owner;
import static com.focusfortress.unit.TestData.progress;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("HabitProgressMapper")
class HabitProgressMapperTest {

    private final Habit habit = habit(42L, owner(), 21);

    @Test
    @DisplayName("toDTO exposes the habit id and an ISO-8601 date")
    void toDto() {
        HabitProgressDTO dto = HabitProgressMapper.toDTO(progress(habit, LocalDate.of(2024, 2, 29)));

        assertThat(dto.getHabitId()).isEqualTo(42L);
        assertThat(dto.getDate()).isEqualTo("2024-02-29");
    }

    @Test
    @DisplayName("toEntity parses the ISO date and links the given habit")
    void toEntity() {
        HabitProgressDTO dto = new HabitProgressDTO();
        dto.setHabitId(42L);
        dto.setDate("2024-12-31");

        HabitProgress entity = HabitProgressMapper.toEntity(dto, habit);

        assertThat(entity.getHabit()).isSameAs(habit);
        assertThat(entity.getDate()).isEqualTo(LocalDate.of(2024, 12, 31));
        assertThat(entity.getId()).isNull();
    }

    @ParameterizedTest(name = "round trip keeps {0}")
    @ValueSource(strings = {"2024-01-01", "2024-02-29", "1999-12-31", "2100-06-15"})
    @DisplayName("entity -> DTO -> entity round trip is lossless")
    void roundTrip(String isoDate) {
        HabitProgress original = progress(habit, LocalDate.parse(isoDate));

        HabitProgress copy = HabitProgressMapper.toEntity(HabitProgressMapper.toDTO(original), habit);

        assertThat(copy.getDate()).isEqualTo(original.getDate());
        assertThat(copy.getHabit()).isSameAs(original.getHabit());
    }

    @ParameterizedTest(name = "\"{0}\" is rejected")
    @ValueSource(strings = {"31-12-2024", "2024/12/31", "2023-02-29", "2024-13-01", "", "today"})
    @DisplayName("toEntity rejects non-ISO or impossible dates")
    void invalidDate(String date) {
        HabitProgressDTO dto = new HabitProgressDTO();
        dto.setDate(date);

        assertThatThrownBy(() -> HabitProgressMapper.toEntity(dto, habit))
                .isInstanceOf(DateTimeParseException.class);
    }
}
