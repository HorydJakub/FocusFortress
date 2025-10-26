package com.focusfortress.service;

import com.focusfortress.dto.CounterDTO;
import com.focusfortress.exception.ForbiddenException;
import com.focusfortress.exception.NotFoundException;
import com.focusfortress.model.Counter;
import com.focusfortress.model.User;
import com.focusfortress.repository.CounterRepository;
import com.focusfortress.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CounterService {

    private final CounterRepository counterRepository;
    private final UserRepository userRepository;

    public CounterService(CounterRepository counterRepository, UserRepository userRepository) {
        this.counterRepository = counterRepository;
        this.userRepository = userRepository;
    }

    public Counter createCounter(String email, CounterDTO counterDTO) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (counterRepository.existsByUserIdAndName(user.getId(), counterDTO.getName())) {
            throw new IllegalArgumentException("Counter with this name already exists for the user");
        }

        Counter counter = Counter.builder()
                .name(counterDTO.getName())
                .description(counterDTO.getDescription())
                .icon(counterDTO.getIcon())
                .user(user)
                .startDateTime(counterDTO.getStartDateTime())
                .build();

        try {
            return counterRepository.save(counter);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("Counter with this name already exists for the user");
        }

    }

    @Transactional(readOnly = true)
    public List<Counter> getUserCounters(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return counterRepository.findByUserId(user.getId());
    }

    public void deleteCounter(Long counterId, String email) {
        Counter counter = counterRepository.findById(counterId)
                .orElseThrow(() -> new NotFoundException("Counter not found"));

        if (!counter.getUser().getEmail().equals(email)) {
            throw new ForbiddenException("Access denied");
        }
        counterRepository.delete(counter);
    }

    public Counter resetCounter(Long counterId, String email) {
        Counter counter = counterRepository.findById(counterId)
                .orElseThrow(() -> new NotFoundException("Counter not found"));

        if (!counter.getUser().getEmail().equals(email)) {
            throw new ForbiddenException("Access denied");
        }

        counter.setStartDateTime(java.time.LocalDateTime.now());
        return counterRepository.save(counter);
    }

    public Counter updateCounter(Long counterId, String email, CounterDTO counterDTO) {
        Counter counter = counterRepository.findById(counterId)
                .orElseThrow(() -> new NotFoundException("Counter not found"));

        if (!counter.getUser().getEmail().equals(email)) {
            throw new ForbiddenException("Access denied");
        }

        if (counterDTO.getName() != null && !counterDTO.getName().equals(counter.getName())) {
            if (counterRepository.existsByUserIdAndName(counter.getUser().getId(), counterDTO.getName())) {
                throw new IllegalArgumentException("Counter with this name already exists for the user");
            }
            counter.setName(counterDTO.getName());
        }

        if (counterDTO.getDescription() != null) {
            counter.setDescription(counterDTO.getDescription());
        }

        if (counterDTO.getIcon() != null) {
            counter.setIcon(counterDTO.getIcon());
        }

        return counterRepository.save(counter);
    }

    @Transactional(readOnly = true)
    public Counter getCounterById(Long counterId, String email) {
        Counter counter = counterRepository.findById(counterId)
                .orElseThrow(() -> new NotFoundException("Counter not found"));
        if (!counter.getUser().getEmail().equals(email)) {
            throw new ForbiddenException("Access denied");
        }
        return counter;
    }
}