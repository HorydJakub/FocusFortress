package com.focusfortress.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "subcategories",
        uniqueConstraints = @UniqueConstraint(columnNames = {"name", "category_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subcategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 5)
    private String icon;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
}