package com.focusfortress.service;

import com.focusfortress.dto.AddMediaItemDTO;
import com.focusfortress.dto.UpdateMediaStatusDTO;
import com.focusfortress.dto.UserMediaItemDTO;
import com.focusfortress.exception.ResourceNotFoundException;
import com.focusfortress.model.MediaStatus;
import com.focusfortress.model.UserMediaItem;
import com.focusfortress.repository.UserMediaItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MediaLibraryService {

    private final UserMediaItemRepository mediaItemRepository;

    public List<UserMediaItemDTO> getUserLibrary(String userEmail) {
        List<UserMediaItem> items = mediaItemRepository.findByUserEmailOrderByAddedAtDesc(userEmail);
        return items.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<UserMediaItemDTO> getUserLibraryByStatus(String userEmail, MediaStatus status) {
        List<UserMediaItem> items = mediaItemRepository.findByUserEmailAndStatus(userEmail, status);
        return items.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserMediaItemDTO addMediaItem(String userEmail, AddMediaItemDTO dto) {
        // Check if video already exists in user's library
        if (mediaItemRepository.existsByUserEmailAndVideoId(userEmail, dto.getVideoId())) {
            throw new IllegalArgumentException("This video is already in your library");
        }

        UserMediaItem item = new UserMediaItem();
        item.setUserEmail(userEmail);
        item.setVideoId(dto.getVideoId());
        item.setTitle(dto.getTitle());
        item.setChannelTitle(dto.getChannelTitle());
        item.setThumbnailUrl(dto.getThumbnailUrl());
        item.setDescription(dto.getDescription());
        item.setMatchedInterest(dto.getMatchedInterest());
        item.setStatus(dto.getStatus() != null ? dto.getStatus() : MediaStatus.WATCH_LATER);
        item.setNotes(dto.getNotes());

        UserMediaItem saved = mediaItemRepository.save(item);
        return convertToDTO(saved);
    }

    @Transactional
    public UserMediaItemDTO updateMediaStatus(String userEmail, Long itemId, UpdateMediaStatusDTO dto) {
        UserMediaItem item = mediaItemRepository.findByIdAndUserEmail(itemId, userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Media item not found"));

        item.setStatus(dto.getStatus());

        if (dto.getNotes() != null) {
            item.setNotes(dto.getNotes());
        }

        UserMediaItem updated = mediaItemRepository.save(item);
        return convertToDTO(updated);
    }

    @Transactional
    public void deleteMediaItem(String userEmail, Long itemId) {
        UserMediaItem item = mediaItemRepository.findByIdAndUserEmail(itemId, userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Media item not found"));

        mediaItemRepository.delete(item);
    }

    private UserMediaItemDTO convertToDTO(UserMediaItem item) {
        UserMediaItemDTO dto = new UserMediaItemDTO();
        dto.setId(item.getId());
        dto.setVideoId(item.getVideoId());
        dto.setTitle(item.getTitle());
        dto.setChannelTitle(item.getChannelTitle());
        dto.setThumbnailUrl(item.getThumbnailUrl());
        dto.setDescription(item.getDescription());
        dto.setMatchedInterest(item.getMatchedInterest());
        dto.setStatus(item.getStatus());
        dto.setNotes(item.getNotes());
        dto.setAddedAt(item.getAddedAt());
        dto.setUpdatedAt(item.getUpdatedAt());
        return dto;
    }
}