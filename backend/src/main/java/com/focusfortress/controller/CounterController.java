package com.focusfortress.controller;

import com.focusfortress.dto.CounterDTO;
import com.focusfortress.model.Counter;
import com.focusfortress.service.CounterService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/counters")
public class CounterController {

    private final CounterService counterService;

    public CounterController(CounterService counterService) {
        this.counterService = counterService;
    }

    @PostMapping
    public ResponseEntity<CounterDTO> createCounter(@Valid @RequestBody CounterDTO counterDTO, Principal principal) {
        Counter saved = counterService.createCounter(principal.getName(), counterDTO);
        URI location = URI.create("/api/counters/" + saved.getId());
        return ResponseEntity.created(location).body(convertToDTO(saved));
    }

    @GetMapping
    public ResponseEntity<List<CounterDTO>> getUserCounters(Principal principal) {
        List<Counter> counters = counterService.getUserCounters(principal.getName());
        return ResponseEntity.ok(
                counters.stream()
                        .map(this::convertToDTO)
                        .collect(Collectors.toList())
        );
    }

    @GetMapping("/{counterId}")
    public ResponseEntity<CounterDTO> getCounterById(@PathVariable("counterId") Long counterId, Principal principal) {
        Counter counter = counterService.getCounterById(counterId, principal.getName());
        return ResponseEntity.ok(convertToDTO(counter));
    }

    @DeleteMapping("/{counterId}")
    public ResponseEntity<Void> deleteCounter(@PathVariable("counterId") Long counterId, Principal principal) {
        counterService.deleteCounter(counterId, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{counterId}/reset")
    public ResponseEntity<CounterDTO> resetCounter(@PathVariable("counterId") Long counterId, Principal principal) {
        Counter updatedCounter = counterService.resetCounter(counterId, principal.getName());
        return ResponseEntity.ok(convertToDTO(updatedCounter));
    }

    private CounterDTO convertToDTO(Counter counter) {
        CounterDTO dto = new CounterDTO();
        dto.setId(counter.getId());
        dto.setName(counter.getName());
        dto.setDescription(counter.getDescription());
        dto.setStartDateTime(counter.getStartDateTime());
        dto.setIcon(counter.getIcon());
        return dto;
    }
}