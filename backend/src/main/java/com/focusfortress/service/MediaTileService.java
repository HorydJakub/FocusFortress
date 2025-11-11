package com.focusfortress.service;

import com.focusfortress.dto.VideoTileDTO;
import com.focusfortress.model.InterestCategory;
import com.focusfortress.model.CategoryStructure;
import com.focusfortress.model.SubcategoryStructure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class MediaTileService {

    private final InterestService interestService;
    private final YouTubeApiClient youTubeApiClient;

    // Dynamically build queries from InterestCategory subcategories
    private static final Map<InterestCategory, List<String>> INTEREST_TO_QUERIES = buildQueryMap();

    private static Map<InterestCategory, List<String>> buildQueryMap() {
        Map<InterestCategory, List<String>> queryMap = new EnumMap<>(InterestCategory.class);

        for (InterestCategory interest : InterestCategory.values()) {
            List<String> queries = new ArrayList<>();

            // Extract all subcategory names as search queries
            for (CategoryStructure category : interest.getCategories()) {
                for (SubcategoryStructure subcategory : category.getSubcategories()) {
                    // Add the subcategory name as a query
                    queries.add(subcategory.getName());

                    // Add enhanced queries with educational context
                    queries.add(subcategory.getName() + " explained");
                    queries.add(subcategory.getName() + " tutorial");
                    queries.add("Learn " + subcategory.getName());
                    queries.add(subcategory.getName() + " basics");
                }
            }

            queryMap.put(interest, queries);
            log.debug("Built {} queries for {}: {}", queries.size(), interest.getDisplayName(), queries);
        }

        return queryMap;
    }

    public List<VideoTileDTO> getPersonalizedTiles(String email, int limit) {
        Set<InterestCategory> userInterests = interestService.getUserInterestCategories(email);

        if (userInterests.isEmpty()) {
            log.warn("User {} has no interests", email);
            return Collections.emptyList();
        }

        List<VideoTileDTO> allTiles = new ArrayList<>();

        // For each interest, get 2-3 videos
        int videosPerInterest = Math.max(2, limit / userInterests.size());

        for (InterestCategory interest : userInterests) {
            List<String> queries = INTEREST_TO_QUERIES.getOrDefault(interest, List.of());
            if (queries.isEmpty()) continue;

            // Randomly select a query from this interest's subcategories
            String randomQuery = queries.get(new Random().nextInt(queries.size()));

            log.info("Fetching YouTube videos for interest {} with query '{}'", interest.getDisplayName(), randomQuery);

            List<VideoTileDTO> tiles = youTubeApiClient.searchEducationalVideos(randomQuery, videosPerInterest);

            // Tag with interest
            tiles.forEach(tile -> {
                tile.setMatchedInterest(interest.getDisplayName());
                tile.setMatchedInterestIcon(interest.getIcon());
            });

            allTiles.addAll(tiles);
        }

        // Shuffle for variety
        Collections.shuffle(allTiles);
        return allTiles.stream().limit(limit).collect(Collectors.toList());
    }

    public List<VideoTileDTO> getTilesByInterest(String email, String interestKey, int limit) {
        InterestCategory interest = InterestCategory.fromString(interestKey);

        Set<InterestCategory> userInterests = interestService.getUserInterestCategories(email);
        if (!userInterests.contains(interest)) {
            throw new IllegalArgumentException("User doesn't have this interest");
        }

        List<String> queries = INTEREST_TO_QUERIES.getOrDefault(interest, List.of());
        List<VideoTileDTO> allTiles = new ArrayList<>();

        // Randomly select subset of queries to get diverse content
        List<String> selectedQueries = new ArrayList<>(queries);
        Collections.shuffle(selectedQueries);

        // Use up to 5 different queries for variety
        int numQueries = Math.min(5, selectedQueries.size());
        int videosPerQuery = Math.max(3, limit / numQueries);

        for (int i = 0; i < numQueries; i++) {
            String query = selectedQueries.get(i);
            log.info("Fetching videos for query: {}", query);

            List<VideoTileDTO> tiles = youTubeApiClient.searchEducationalVideos(query, videosPerQuery);

            tiles.forEach(tile -> {
                tile.setMatchedInterest(interest.getDisplayName());
                tile.setMatchedInterestIcon(interest.getIcon());
            });

            allTiles.addAll(tiles);
        }

        Collections.shuffle(allTiles);
        return allTiles.stream().limit(limit).collect(Collectors.toList());
    }
}
