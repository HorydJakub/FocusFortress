package com.focusfortress.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "report_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReportSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "include_active_habits")
    private boolean includeActiveHabits = true;

    @Column(name = "include_completed_habits")
    private boolean includeCompletedHabits = true;

    @Column(name = "include_interests")
    private boolean includeInterests = true;

    @Column(name = "include_media_library")
    private boolean includeMediaLibrary = true;

    @Column(name = "include_counters")
    private boolean includeCounters = true;

    @Column(name = "automatic_reports")
    private boolean automaticReports = false;

    @Column(name = "report_frequency")
    private String reportFrequency = "weekly"; // "weekly" or "monthly"
}

