package com.focusfortress.service;

import com.focusfortress.dto.CounterDTO;
import com.focusfortress.model.Counter;
import com.focusfortress.model.User;
import com.focusfortress.repository.CounterRepository;
import com.focusfortress.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CounterService {

    private CounterRepository counterRepository;
    private UserRepository userRepository;

    public CounterService(CounterRepository counterRepository, UserRepository userRepository) {
        this.counterRepository = counterRepository;
        this.userRepository = userRepository;
    }

    public Counter createCounter(String email, CounterDTO counterDTO) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Counter counter = new Counter();
        counter.setName(counterDTO.getName());
        counter.setDescription(counterDTO.getDescription());
        counter.setStartDateTime(counterDTO.getStartDateTime());
        counter.setIcon(counterDTO.getIcon());
        counter.setUser(user);

        return counterRepository.save(counter);
    }

    public List<Counter> getUserCounters(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return counterRepository.findByUserId(user.getId());
    }

    public void deleteCounter(Long counterId, String email) {
        Counter counter = counterRepository.findById(counterId)
                .orElseThrow(() -> new RuntimeException("Counter not found"));
        if (!counter.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Access denied!");
        }
        counterRepository.delete(counter);
    }

    public void resetCounter(Long counterId, String email) {
        Counter counter = counterRepository.findById(counterId)
                .orElseThrow(() -> new RuntimeException("Counter not found"));
        if (!counter.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Access denied");
        }
        counter.setStartDateTime(java.time.LocalDateTime.now());
        counterRepository.save(counter);
    }

    public Counter getCounterById(Long counterId, String email) {
        Counter counter = counterRepository.findById(counterId)
                .orElseThrow(() -> new RuntimeException("Counter not found"));
        if (!counter.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Access denied");
        }
        return counter;
    }
}