package com.focusfortress.repository;

import com.focusfortress.model.Counter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CounterRepository extends JpaRepository<Counter, Long> {
    List<Counter> findByUserId(Long userId);
}