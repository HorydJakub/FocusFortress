package com.focusfortress.config;

import com.focusfortress.model.*;
import com.focusfortress.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final UserInterestRepository userInterestRepository;
    private final HabitRepository habitRepository;
    private final HabitProgressRepository habitProgressRepository;
    private final CounterRepository counterRepository;
    // Usunięto repository mediów zgodnie z życzeniem

    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // 1. Najpierw instalujemy logikę bazy danych (niezależnie od danych)
        initTriggersAndEvents();

        // 2. Potem dodajemy dane (tylko jeśli baza jest pusta)
        if (categoryRepository.count() == 0) {
            seedData();
        }
    }

    private void initTriggersAndEvents() {
        System.out.println("--- INSTALACJA LOGIKI SQL (EVENTS & TRIGGERS) ---");

        // EVENT 1: Czyszczenie kont
        try {
            jdbcTemplate.execute("DROP EVENT IF EXISTS clean_unverified_users");
            jdbcTemplate.execute("""
                CREATE EVENT clean_unverified_users
                ON SCHEDULE EVERY 1 DAY
                STARTS CURRENT_TIMESTAMP
                DO
                  DELETE FROM users 
                  WHERE role = 'UNVERIFIED' 
                  AND created_at < NOW() - INTERVAL 24 HOUR
            """);
            System.out.println("Event 'clean_unverified_users' zainstalowany.");
        } catch (Exception e) {
            System.err.println("Błąd przy Event clean_unverified_users: " + e.getMessage());
        }

        // TRIGGER 1: Blokada usuwania
        try {
            jdbcTemplate.execute("DROP TRIGGER IF EXISTS prevent_habit_deletion_with_history");
            jdbcTemplate.execute("""
                CREATE TRIGGER prevent_habit_deletion_with_history
                BEFORE DELETE ON habits
                FOR EACH ROW
                BEGIN
                    DECLARE progress_count INT;
                    SELECT COUNT(*) INTO progress_count FROM habit_progress WHERE habit_id = OLD.id;
                    IF progress_count > 0 THEN
                        SIGNAL SQLSTATE '45000' 
                        SET MESSAGE_TEXT = 'SECURITY: Nie można usunąć nawyku, który posiada historię!';
                    END IF;
                END;
            """);
            System.out.println("Trigger 'prevent_habit_deletion_with_history' zainstalowany.");
        } catch (Exception e) {
            System.err.println("Błąd przy Trigger prevent_habit_deletion_with_history: " + e.getMessage());
        }

        // TRIGGER 2: Loguje każdą zmianę roli użytkownika (np. z USER na ADMIN)
                try {
                    jdbcTemplate.execute("DROP TRIGGER IF EXISTS log_role_changes");
                    jdbcTemplate.execute("""
                        CREATE TRIGGER log_role_changes
                        AFTER UPDATE ON users
                        FOR EACH ROW
                        BEGIN
                            IF OLD.role != NEW.role THEN
                                INSERT INTO admin_audit_logs (user_email, old_role, new_role, action_note)
                                VALUES (NEW.email, OLD.role, NEW.role, 'Zmiana uprawnień wykryta przez system');
                            END IF;
                        END;
                    """);
                    System.out.println("Trigger 'log_role_changes' zainstalowany.");
                } catch (Exception e) {
                    System.err.println("Błąd przy Trigger log_role_changes: " + e.getMessage());
                }
    }

    private void seedData() {
        System.out.println("--- ROZPOCZYNAM SEEDOWANIE DANYCH ---");

        // --- 1. KATEGORIE ---
        Category health = Category.builder().name("Zdrowie").icon("❤️").build();
        Category productivity = Category.builder().name("Produktywność").icon("🚀").build();
        Category education = Category.builder().name("Edukacja").icon("📚").build();
        categoryRepository.saveAll(List.of(health, productivity, education));

        Subcategory gym = Subcategory.builder().name("Siłownia").category(health).icon("🏋️").build();
        Subcategory diet = Subcategory.builder().name("Dieta").category(health).icon("🥦").build();
        Subcategory sleep = Subcategory.builder().name("Sen").category(health).icon("🌙").build();
        Subcategory coding = Subcategory.builder().name("Programowanie").category(education).icon("💻").build();
        Subcategory reading = Subcategory.builder().name("Czytanie").category(education).icon("📖").build();
        Subcategory deepWork = Subcategory.builder().name("Deep Work").category(productivity).icon("🧠").build();
        subcategoryRepository.saveAll(List.of(gym, diet, sleep, coding, reading, deepWork));

        User user = new User();
        user.setName("Test User");
        user.setEmail("user@test.pl");
        user.setPassword(passwordEncoder.encode("password123")); // 8+ znaków
        user.setRole(Role.USER);
        user.setProvider(AuthProvider.LOCAL);
        user.setDateOfBirth(LocalDate.of(1995, 5, 20));
        user.setBio("Chcę przejąć kontrolę nad swoim czasem.");

        userRepository.saveAll(List.of(user));

        // Zainteresowania
        UserInterest i1 = UserInterest.builder().user(user).subcategory(gym).build();
        UserInterest i2 = UserInterest.builder().user(user).subcategory(coding).build();
        UserInterest i3 = UserInterest.builder().user(user).subcategory(deepWork).build();
        userInterestRepository.saveAll(List.of(i1, i2, i3));

        // --- 3. NAWYKI (5 Sztuk) ---

        // Nawyk 1: Jogging (Zdrowie)
        Habit h1 = Habit.builder()
                .name("Poranny Jogging")
                .description("3km biegania z rana")
                .user(user).category(health).subcategory(gym)
                .durationDays(30).icon("🏃").done(false).build();

        // Nawyk 2: Kodowanie (Edukacja)
        Habit h2 = Habit.builder()
                .name("Nauka Spring Boot")
                .description("Minimum 1 commit dziennie")
                .user(user).category(education).subcategory(coding)
                .durationDays(90).icon("☕").done(false).build();

        // Nawyk 3: Woda (Dieta)
        Habit h3 = Habit.builder()
                .name("Picie Wody")
                .description("2 litry wody dziennie")
                .user(user).category(health).subcategory(diet)
                .durationDays(60).icon("💧").done(false).build();

        // Nawyk 4: Czytanie (Edukacja)
        Habit h4 = Habit.builder()
                .name("Czytanie przed snem")
                .description("10 stron książki niefabularnej")
                .user(user).category(education).subcategory(reading)
                .durationDays(21).icon("📖").done(false).build();

        // Nawyk 5: Medytacja (Produktywność - Deep Work)
        Habit h5 = Habit.builder()
                .name("Medytacja")
                .description("10 minut ciszy")
                .user(user).category(productivity).subcategory(deepWork)
                .durationDays(100).icon("🧘").done(false).build();

        habitRepository.saveAll(List.of(h1, h2, h3, h4, h5));

        // --- 4. HISTORIA POSTĘPÓW (Symulacja danych "w trakcie") ---

        // H1 (Bieganie) - biegane było wczoraj i 3 dni temu
        createProgress(h1, 1);
        createProgress(h1, 3);

        // H2 (Kodowanie) - idzie świetnie, ostatnie 3 dni pod rząd
        createProgress(h2, 0); // Dzisiaj
        createProgress(h2, 1); // Wczoraj
        createProgress(h2, 2); // Przedwczoraj

        // H3 (Woda) - w kratkę
        createProgress(h3, 1);
        createProgress(h3, 4);
        createProgress(h3, 5);

        // --- 5. LICZNIKI (3 Sztuki - "W trakcie") ---

        // Licznik 1: Bez cukru (zaczęte 5 dni temu)
        Counter c1 = Counter.builder()
                .name("Dni bez cukru")
                .description("Ograniczam słodycze")
                .user(user)
                .icon("🍬")
                .startDateTime(LocalDateTime.now().minusDays(5).minusHours(2)) // 5 dni i 2h temu
                .build();

        // Licznik 2: Nauka ciągiem (zaczęte 4 godziny temu - sesja deep work)
        Counter c2 = Counter.builder()
                .name("Sesja Deep Work")
                .description("Czas skupienia bez telefonu")
                .user(user)
                .icon("🧠")
                .startDateTime(LocalDateTime.now().minusHours(4).minusMinutes(15)) // 4h 15min temu
                .build();

        // Licznik 3: Bez papierosa (zaczęte miesiąc temu)
        Counter c3 = Counter.builder()
                .name("Wolność od nikotyny")
                .description("Rzucam palenie")
                .user(user)
                .icon("🚭")
                .startDateTime(LocalDateTime.now().minusDays(30)) // Równo 30 dni
                .build();

        counterRepository.saveAll(List.of(c1, c2, c3));

        System.out.println("--- 🌳 DANE TESTOWE ZAŁADOWANE (5 nawyków, 3 liczniki) ---");
        System.out.println("--- Logowanie: user@test.pl / password123 ---");
    }

    // Metoda pomocnicza do tworzenia historii
    private void createProgress(Habit habit, int daysAgo) {
        HabitProgress p = new HabitProgress();
        p.setHabit(habit);
        p.setDate(LocalDate.now().minusDays(daysAgo));
        habitProgressRepository.save(p);
    }
}