package com.focusfortress.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users", uniqueConstraints = {@UniqueConstraint(columnNames = "email")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 50, message = "Name should be between 2 and 50 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    private LocalDate dateOfBirth;

    private String gender;

    // Password is optional (nullable) for OAuth2 users
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @Enumerated(EnumType.STRING)
    private AuthProvider provider;

    private String verificationToken;

    @Enumerated(EnumType.STRING)
    private Role role = Role.UNVERIFIED;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime lastLoginAt;

    private String avatarUrl;

    @Column(length = 500)
    private String bio;

    private String timezone;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserInterest> interests = new HashSet<>();

    public boolean isVerified() {
        return role != Role.UNVERIFIED;
    }

    // constructor is used by UserService.registerUser()
    public User(String name, String email, LocalDate dateOfBirth, String gender, String password, String verificationToken) {
        this.name = name;
        this.email = email;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.password = password;
        this.verificationToken = verificationToken;
        this.role = Role.UNVERIFIED;
        this.provider = AuthProvider.LOCAL; // Default to LOCAL for standard registration
    }

    // Helper methods for managing interests
    public void addInterest(UserInterest interest) {
        interests.add(interest);
        interest.setUser(this);
    }

    public void removeInterest(UserInterest interest) {
        interests.remove(interest);
        interest.setUser(null);
    }
}