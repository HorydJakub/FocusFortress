package com.focusfortress.service;

import com.focusfortress.dto.VideoTileDTO;
import com.focusfortress.youtube.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class YouTubeApiClient {

    private final WebClient webClient;
    private final String apiKey;

    public YouTubeApiClient(
            @Value("${youtube.api.key}") String apiKey,
            @Value("${youtube.api.base-url}") String baseUrl) {
        this.apiKey = apiKey;
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    /**
     * Search YouTube videos with educational filter
     */
    public List<VideoTileDTO> searchEducationalVideos(String query, int maxResults) {
        try {
            log.info("Calling YouTube API with query: '{}', maxResults: {}", query, maxResults);

            YouTubeSearchResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search")
                            .queryParam("part", "snippet")
                            .queryParam("q", query)
                            .queryParam("type", "video")
                            .queryParam("videoCategoryId", "27")  // 27 = Education
                            .queryParam("maxResults", Math.min(maxResults, 50))
                            .queryParam("safeSearch", "strict")
                            .queryParam("videoEmbeddable", "true")
                            .queryParam("key", apiKey)
                            .build())
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .map(errorBody -> {
                                        log.error("YouTube API error response: {}", errorBody);
                                        return new RuntimeException("YouTube API error: " + errorBody);
                                    }))
                    .bodyToMono(YouTubeSearchResponse.class)
                    .block();

            if (response == null || response.getItems() == null || response.getItems().isEmpty()) {
                log.warn("No results from YouTube API for query: {} with category filter, trying without category", query);
                // try without category restriction
                return searchWithoutCategory(query, maxResults);
            }

            List<VideoTileDTO> results = response.getItems().stream()
                    .filter(item -> item.getId() != null && item.getId().getVideoId() != null)
                    .map(this::convertToVideoTile)
                    .collect(Collectors.toList());

            log.info("YouTube API returned {} videos for query '{}'", results.size(), query);
            return results;

        } catch (Exception e) {
            log.error("Error calling YouTube API for query '{}': {}", query, e.getMessage(), e);
            // ry without category restriction
            log.info("Attempting fallback without category restriction for query: {}", query);
            return searchWithoutCategory(query, maxResults);
        }
    }

    private List<VideoTileDTO> searchWithoutCategory(String query, int maxResults) {
        try {
            log.info("Calling YouTube API WITHOUT category filter for query: '{}'", query);

            YouTubeSearchResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search")
                            .queryParam("part", "snippet")
                            .queryParam("q", query)
                            .queryParam("type", "video")
                            .queryParam("maxResults", Math.min(maxResults, 50))
                            .queryParam("safeSearch", "strict")
                            .queryParam("videoEmbeddable", "true")
                            .queryParam("relevanceLanguage", "en")
                            .queryParam("key", apiKey)
                            .build())
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .map(errorBody -> {
                                        log.error("YouTube API fallback error response: {}", errorBody);
                                        return new RuntimeException("YouTube API error: " + errorBody);
                                    }))
                    .bodyToMono(YouTubeSearchResponse.class)
                    .block();

            if (response == null || response.getItems() == null) {
                log.warn("No results from YouTube API fallback for query: {}", query);
                return Collections.emptyList();
            }

            List<VideoTileDTO> results = response.getItems().stream()
                    .filter(item -> item.getId() != null && item.getId().getVideoId() != null)
                    .map(this::convertToVideoTile)
                    .collect(Collectors.toList());

            log.info("YouTube API fallback returned {} videos for query '{}'", results.size(), query);
            return results;

        } catch (Exception e) {
            log.error("Error calling YouTube API fallback for query '{}': {}", query, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Search with broader filter (no category restriction)
     */
    public List<VideoTileDTO> searchEducationalVideosMultiCategory(String query, int maxResults) {
        try {
            YouTubeSearchResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search")
                            .queryParam("part", "snippet")
                            .queryParam("q", query)
                            .queryParam("type", "video")
                            .queryParam("maxResults", Math.min(maxResults, 50))
                            .queryParam("safeSearch", "strict")
                            .queryParam("videoEmbeddable", "true")
                            .queryParam("relevanceLanguage", "en")
                            .queryParam("key", apiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(YouTubeSearchResponse.class)
                    .block();

            if (response == null || response.getItems() == null) {
                return Collections.emptyList();
            }

            return response.getItems().stream()
                    .filter(item -> item.getId() != null && item.getId().getVideoId() != null)
                    .filter(this::isEducationalContent)
                    .map(this::convertToVideoTile)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error calling YouTube API: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private boolean isEducationalContent(YouTubeSearchItem item) {
        String title = item.getSnippet().getTitle().toLowerCase();
        String description = item.getSnippet().getDescription().toLowerCase();
        String channelTitle = item.getSnippet().getChannelTitle().toLowerCase();

        List<String> educationalKeywords = List.of(
                "lecture", "tutorial", "course", "learn", "education",
                "explained", "guide", "documentary", "science", "philosophy",
                "ted", "university", "professor", "academy"
        );

        List<String> spamKeywords = List.of(
                "clickbait", "drama", "exposed", "reaction", "prank",
                "vs", "diss", "roast", "cringe"
        );

        String combined = title + " " + description + " " + channelTitle;

        boolean hasEducationalKeyword = educationalKeywords.stream()
                .anyMatch(combined::contains);

        boolean hasSpamKeyword = spamKeywords.stream()
                .anyMatch(combined::contains);

        return hasEducationalKeyword && !hasSpamKeyword;
    }

    private VideoTileDTO convertToVideoTile(YouTubeSearchItem item) {
        VideoTileDTO dto = new VideoTileDTO();
        dto.setVideoId(item.getId().getVideoId());
        dto.setTitle(item.getSnippet().getTitle());
        dto.setChannelTitle(item.getSnippet().getChannelTitle());
        dto.setDescription(item.getSnippet().getDescription());

        if (item.getSnippet().getThumbnails() != null) {
            if (item.getSnippet().getThumbnails().getHigh() != null) {
                dto.setThumbnailUrl(item.getSnippet().getThumbnails().getHigh().getUrl());
            } else if (item.getSnippet().getThumbnails().getMedium() != null) {
                dto.setThumbnailUrl(item.getSnippet().getThumbnails().getMedium().getUrl());
            }
        }

        return dto;
    }
}

