package com.focusfortress.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "counters",
        uniqueConstraints = @UniqueConstraint(name = "uk_counters_user_name", columnNames = {"user_id", "name"})
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Counter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 128)
    private String name;

    @Column(length = 512)
    private String description;

    private LocalDateTime startDateTime;

    @Column(length = 128)
    private String icon;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @PrePersist
    void prePersist() {
        if (startDateTime == null) {
            startDateTime = LocalDateTime.now();
        }
    }
}
