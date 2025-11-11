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
}