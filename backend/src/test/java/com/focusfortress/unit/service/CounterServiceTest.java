package com.focusfortress.unit.service;

import com.focusfortress.dto.CounterDTO;
import com.focusfortress.exception.ForbiddenException;
import com.focusfortress.exception.NotFoundException;
import com.focusfortress.model.Counter;
import com.focusfortress.model.User;
import com.focusfortress.repository.CounterRepository;
import com.focusfortress.repository.UserRepository;
import com.focusfortress.service.CounterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.ThrowingConsumer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Stream;

import static com.focusfortress.unit.TestData.OTHER_EMAIL;
import static com.focusfortress.unit.TestData.OWNER_EMAIL;
import static com.focusfortress.unit.TestData.counter;
import static com.focusfortress.unit.TestData.owner;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CounterService")
class CounterServiceTest {

    @Mock
    private CounterRepository counterRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CounterService service;

    private User owner;

    @BeforeEach
    void setUp() {
        owner = owner();
    }

    private static CounterDTO dto(String name, String description, String icon, LocalDateTime start) {
        CounterDTO dto = new CounterDTO();
        dto.setName(name);
        dto.setDescription(description);
        dto.setIcon(icon);
        dto.setStartDateTime(start);
        return dto;
    }

    private void givenSaveEchoesEntity() {
        when(counterRepository.save(any(Counter.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Nested
    @DisplayName("createCounter")
    class Create {

        @Test
        @DisplayName("creates a counter owned by the caller with the given start time")
        void creates() {
            LocalDateTime start = LocalDateTime.of(2024, 5, 1, 12, 0);
            when(userRepository.findByEmail(OWNER_EMAIL)).thenReturn(Optional.of(owner));
            givenSaveEchoesEntity();

            Counter created = service.createCounter(OWNER_EMAIL, dto("No sugar", "since May", "S", start));

            assertThat(created.getName()).isEqualTo("No sugar");
            assertThat(created.getDescription()).isEqualTo("since May");
            assertThat(created.getIcon()).isEqualTo("S");
            assertThat(created.getStartDateTime()).isEqualTo(start);
            assertThat(created.getUser()).isSameAs(owner);
        }

        @Test
        @DisplayName("rejects a duplicate name detected up-front")
        void duplicateByLookup() {
            when(userRepository.findByEmail(OWNER_EMAIL)).thenReturn(Optional.of(owner));
            when(counterRepository.existsByUserIdAndName(owner.getId(), "No sugar")).thenReturn(true);

            assertThatThrownBy(() -> service.createCounter(OWNER_EMAIL, dto("No sugar", null, null, null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already exists");
            verify(counterRepository, never()).save(any());
        }

        @Test
        @DisplayName("translates a unique-constraint race (DataIntegrityViolation) into IllegalArgumentException")
        void duplicateByConstraint() {
            when(userRepository.findByEmail(OWNER_EMAIL)).thenReturn(Optional.of(owner));
            when(counterRepository.save(any(Counter.class)))
                    .thenThrow(new DataIntegrityViolationException("uk_counters_user_name"));

            assertThatThrownBy(() -> service.createCounter(OWNER_EMAIL, dto("No sugar", null, null, null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already exists");
        }

        @Test
        @DisplayName("fails for an unknown user")
        void unknownUser() {
            when(userRepository.findByEmail(OWNER_EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createCounter(OWNER_EMAIL, dto("x", null, null, null)))
                    .isInstanceOf(UsernameNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateCounter")
    class Update {

        private Counter existing;

        @BeforeEach
        void setUp() {
            existing = counter(5L, owner, "Original");
        }

        @Test
        @DisplayName("applies only non-null fields (partial update)")
        void partialUpdate() {
            when(counterRepository.findById(5L)).thenReturn(Optional.of(existing));
            givenSaveEchoesEntity();

            Counter updated = service.updateCounter(5L, OWNER_EMAIL, dto(null, "new description", null, null));

            assertThat(updated.getName()).isEqualTo("Original");
            assertThat(updated.getDescription()).isEqualTo("new description");
            assertThat(updated.getIcon()).isEqualTo("icon");
            verify(counterRepository, never()).existsByUserIdAndName(anyLong(), anyString());
        }

        @Test
        @DisplayName("never changes the start time (only reset does)")
        void startTimeIsImmutable() {
            LocalDateTime originalStart = existing.getStartDateTime();
            when(counterRepository.findById(5L)).thenReturn(Optional.of(existing));
            givenSaveEchoesEntity();

            Counter updated = service.updateCounter(5L, OWNER_EMAIL,
                    dto(null, null, null, LocalDateTime.of(2000, 1, 1, 0, 0)));

            assertThat(updated.getStartDateTime()).isEqualTo(originalStart);
        }

        @Test
        @DisplayName("skips the uniqueness check when the name is unchanged")
        void sameNameNoUniquenessCheck() {
            when(counterRepository.findById(5L)).thenReturn(Optional.of(existing));
            givenSaveEchoesEntity();

            service.updateCounter(5L, OWNER_EMAIL, dto("Original", null, "new-icon", null));

            verify(counterRepository, never()).existsByUserIdAndName(anyLong(), anyString());
            assertThat(existing.getIcon()).isEqualTo("new-icon");
        }

        @Test
        @DisplayName("renames when the new name is free")
        void rename() {
            when(counterRepository.findById(5L)).thenReturn(Optional.of(existing));
            when(counterRepository.existsByUserIdAndName(owner.getId(), "Renamed")).thenReturn(false);
            givenSaveEchoesEntity();

            assertThat(service.updateCounter(5L, OWNER_EMAIL, dto("Renamed", null, null, null)).getName())
                    .isEqualTo("Renamed");
        }

        @Test
        @DisplayName("rejects renaming to a name the user already uses")
        void renameToTakenName() {
            when(counterRepository.findById(5L)).thenReturn(Optional.of(existing));
            when(counterRepository.existsByUserIdAndName(owner.getId(), "Taken")).thenReturn(true);

            assertThatThrownBy(() -> service.updateCounter(5L, OWNER_EMAIL, dto("Taken", null, null, null)))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(existing.getName()).isEqualTo("Original");
            verify(counterRepository, never()).save(any());
        }
    }

    @Test
    @DisplayName("resetCounter moves the start time to 'now'")
    void reset() {
        Counter existing = counter(5L, owner, "Original");
        when(counterRepository.findById(5L)).thenReturn(Optional.of(existing));
        givenSaveEchoesEntity();

        LocalDateTime before = LocalDateTime.now();
        Counter reset = service.resetCounter(5L, OWNER_EMAIL);
        LocalDateTime after = LocalDateTime.now();

        assertThat(reset.getStartDateTime()).isBetween(before, after);
    }

    @Test
    @DisplayName("getCounterById returns the owner's counter")
    void getById() {
        Counter existing = counter(5L, owner, "Original");
        when(counterRepository.findById(5L)).thenReturn(Optional.of(existing));

        assertThat(service.getCounterById(5L, OWNER_EMAIL)).isSameAs(existing);
    }

    @Test
    @DisplayName("deleteCounter removes the owner's counter")
    void delete() {
        Counter existing = counter(5L, owner, "Original");
        when(counterRepository.findById(5L)).thenReturn(Optional.of(existing));

        service.deleteCounter(5L, OWNER_EMAIL);

        verify(counterRepository).delete(existing);
    }

    static Stream<Arguments> ownershipGuardedOperations() {
        CounterDTO anyDto = dto("x", null, null, null);
        return Stream.of(
                Arguments.of("getCounterById", (ThrowingConsumer<CounterService>) s -> s.getCounterById(5L, OTHER_EMAIL)),
                Arguments.of("updateCounter", (ThrowingConsumer<CounterService>) s -> s.updateCounter(5L, OTHER_EMAIL, anyDto)),
                Arguments.of("resetCounter", (ThrowingConsumer<CounterService>) s -> s.resetCounter(5L, OTHER_EMAIL)),
                Arguments.of("deleteCounter", (ThrowingConsumer<CounterService>) s -> s.deleteCounter(5L, OTHER_EMAIL)));
    }

    @ParameterizedTest(name = "{0} on a foreign counter -> 403")
    @MethodSource("ownershipGuardedOperations")
    @DisplayName("every single-counter operation enforces ownership")
    void ownershipIsEnforced(String operation, ThrowingConsumer<CounterService> call) {
        when(counterRepository.findById(5L)).thenReturn(Optional.of(counter(5L, owner, "Original")));

        assertThatThrownBy(() -> call.accept(service))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Access denied");
        verify(counterRepository, never()).save(any());
        verify(counterRepository, never()).delete(any());
    }

    static Stream<Arguments> lookupOperations() {
        CounterDTO anyDto = dto("x", null, null, null);
        return Stream.of(
                Arguments.of("getCounterById", (ThrowingConsumer<CounterService>) s -> s.getCounterById(5L, OWNER_EMAIL)),
                Arguments.of("updateCounter", (ThrowingConsumer<CounterService>) s -> s.updateCounter(5L, OWNER_EMAIL, anyDto)),
                Arguments.of("resetCounter", (ThrowingConsumer<CounterService>) s -> s.resetCounter(5L, OWNER_EMAIL)),
                Arguments.of("deleteCounter", (ThrowingConsumer<CounterService>) s -> s.deleteCounter(5L, OWNER_EMAIL)));
    }

    @ParameterizedTest(name = "{0} on a missing counter -> 404")
    @MethodSource("lookupOperations")
    @DisplayName("every single-counter operation reports a missing counter")
    void missingCounter(String operation, ThrowingConsumer<CounterService> call) {
        when(counterRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> call.accept(service))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Counter not found");
    }
}
