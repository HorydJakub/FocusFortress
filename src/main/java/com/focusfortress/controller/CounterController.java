package com.focusfortress.controller;

import com.focusfortress.dto.CounterDTO;
import com.focusfortress.model.Counter;
import com.focusfortress.service.CounterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/counters")
public class CounterController {

    @Autowired
    private CounterService counterService;

    @PostMapping
    public ResponseEntity<CounterDTO> createCounter(@RequestBody CounterDTO counterDTO, Principal principal) {
        Counter counter = counterService.createCounter(principal.getName(), counterDTO);
        return ResponseEntity.ok(convertToDTO(counter));
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

    @DeleteMapping("/{counterId}")
    public ResponseEntity<Void> deleteCounter(@PathVariable("counterId") Long counterId, Principal principal) {
        counterService.deleteCounter(counterId, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{counterId}/reset")
    public ResponseEntity<CounterDTO> resetCounter(@PathVariable("counterId") Long counterId, Principal principal) {
        counterService.resetCounter(counterId, principal.getName());
        Counter updatedCounter = counterService.getCounterById(counterId, principal.getName());
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