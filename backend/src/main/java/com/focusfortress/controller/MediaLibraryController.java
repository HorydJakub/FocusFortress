package com.focusfortress.controller;

import com.focusfortress.dto.AddMediaItemDTO;
import com.focusfortress.dto.UpdateMediaStatusDTO;
import com.focusfortress.dto.UserMediaItemDTO;
import com.focusfortress.model.MediaStatus;
import com.focusfortress.service.MediaLibraryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/media/library")
@RequiredArgsConstructor
public class MediaLibraryController {

    private final MediaLibraryService mediaLibraryService;

    /**
     * GET /api/media/library
     * Get all media items in user's library
     */
    @GetMapping
    public ResponseEntity<List<UserMediaItemDTO>> getUserLibrary(
            @RequestParam(name = "status", required = false) MediaStatus status,
            Principal principal) {

        if (status != null) {
            return ResponseEntity.ok(
                    mediaLibraryService.getUserLibraryByStatus(principal.getName(), status)
            );
        }

        return ResponseEntity.ok(mediaLibraryService.getUserLibrary(principal.getName()));
    }

    /**
     * POST /api/media/library
     * Add a new media item to library
     */
    @PostMapping
    public ResponseEntity<UserMediaItemDTO> addMediaItem(
            @Valid @RequestBody AddMediaItemDTO dto,
            Principal principal) {

        UserMediaItemDTO created = mediaLibraryService.addMediaItem(principal.getName(), dto);
        URI location = URI.create("/api/media/library/" + created.getId());
        return ResponseEntity.created(location).body(created);
    }

    /**
     * PATCH /api/media/library/{id}/status
     * Update media item status
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<UserMediaItemDTO> updateMediaStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateMediaStatusDTO dto,
            Principal principal) {

        UserMediaItemDTO updated = mediaLibraryService.updateMediaStatus(
                principal.getName(),
                id,
                dto
        );
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/media/library/{id}
     * Remove media item from library
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMediaItem(
            @PathVariable("id") Long id,
            Principal principal) {

        mediaLibraryService.deleteMediaItem(principal.getName(), id);
        return ResponseEntity.noContent().build();
    }
}