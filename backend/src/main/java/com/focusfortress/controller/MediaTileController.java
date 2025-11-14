package com.focusfortress.controller;

import com.focusfortress.dto.VideoTileDTO;
import com.focusfortress.service.MediaTileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/media/tiles")
@RequiredArgsConstructor
public class MediaTileController {

    private final MediaTileService mediaTileService;

    @GetMapping
    public ResponseEntity<List<VideoTileDTO>> getPersonalizedTiles(
            @RequestParam(name = "limit", defaultValue = "12") int limit,
            Principal principal) {

        return ResponseEntity.ok(
                mediaTileService.getPersonalizedTiles(principal.getName(), limit)
        );
    }

    /**
     * GET /api/media/tiles/custom
     * Returns video recommendations based on a list of specific subcategory names
     * This allows users to refresh content with their currently selected interests
     */
    @GetMapping("/custom")
    public ResponseEntity<List<VideoTileDTO>> getCustomTiles(
            @RequestParam(name = "subcategories") List<String> subcategoryNames,
            @RequestParam(name = "limit", defaultValue = "12") int limit,
            Principal principal) {

        return ResponseEntity.ok(
                mediaTileService.getTilesBySubcategories(principal.getName(), subcategoryNames, limit)
        );
    }
}