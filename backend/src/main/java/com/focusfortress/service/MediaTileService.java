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

    // Map subcategory names to search queries
    private static final Map<String, List<String>> SUBCATEGORY_TO_QUERIES = buildQueryMap();

    private static Map<String, List<String>> buildQueryMap() {
        Map<String, List<String>> queryMap = new HashMap<>();

        for (InterestCategory interest : InterestCategory.values()) {
            // Extract all subcategory names as search queries
            for (CategoryStructure category : interest.getCategories()) {
                for (SubcategoryStructure subcategory : category.getSubcategories()) {
                    String subcategoryName = subcategory.getName();
                    List<String> queries = new ArrayList<>();

                    // Add the subcategory name as a query
                    queries.add(subcategoryName);

                    // Add enhanced queries with educational context
                    queries.add(subcategoryName + " explained");
                    queries.add(subcategoryName + " tutorial");
                    queries.add("Learn " + subcategoryName);
                    queries.add(subcategoryName + " basics");

                    queryMap.put(subcategoryName, queries);
                }
            }
        }

        log.debug("Built query map for {} subcategories", queryMap.size());
        return queryMap;
    }

    public List<VideoTileDTO> getPersonalizedTiles(String email, int limit) {
        Set<String> userSubcategories = interestService.getUserSubcategoryNames(email);

        if (userSubcategories.isEmpty()) {
            log.warn("User {} has no interests", email);
            return Collections.emptyList();
        }

        List<VideoTileDTO> allTiles = new ArrayList<>();

        // For each subcategory, get 2-3 videos
        int videosPerSubcategory = Math.max(2, limit / userSubcategories.size());

        for (String subcategoryName : userSubcategories) {
            List<String> queries = SUBCATEGORY_TO_QUERIES.getOrDefault(subcategoryName, List.of());
            if (queries.isEmpty()) continue;

            // Randomly select a query from this subcategory's queries
            String randomQuery = queries.get(new Random().nextInt(queries.size()));

            log.info("Fetching YouTube videos for subcategory {} with query '{}'", subcategoryName, randomQuery);

            List<VideoTileDTO> tiles = youTubeApiClient.searchEducationalVideos(randomQuery, videosPerSubcategory);

            // Tag with subcategory
            tiles.forEach(tile -> {
                tile.setMatchedInterest(subcategoryName);
            });

            allTiles.addAll(tiles);
        }

        // Shuffle for variety
        Collections.shuffle(allTiles);
        return allTiles.stream().limit(limit).collect(Collectors.toList());
    }

    public List<VideoTileDTO> getTilesBySubcategory(String email, String subcategoryName, int limit) {
        Set<String> userSubcategories = interestService.getUserSubcategoryNames(email);

        if (!userSubcategories.contains(subcategoryName)) {
            throw new IllegalArgumentException("User doesn't have this interest");
        }

        List<String> queries = SUBCATEGORY_TO_QUERIES.getOrDefault(subcategoryName, List.of());
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
                tile.setMatchedInterest(subcategoryName);
            });

            allTiles.addAll(tiles);
        }

        Collections.shuffle(allTiles);
        return allTiles.stream().limit(limit).collect(Collectors.toList());
    }
}
