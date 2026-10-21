package com.focusfortress.api.support;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Valid request bodies for the resource endpoints. Each factory returns a fresh, mutable map,
 * so a test can break exactly one field with {@code put/remove} and keep the rest valid.
 */
public final class Payloads {

    private Payloads() {
    }

    /** Short unique suffix keeps names readable in logs while avoiding collisions between tests. */
    public static String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    public static Map<String, Object> habit(String name) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("description", "Read 10 pages every day");
        body.put("icon", "📖");
        body.put("durationDays", 21);
        return body;
    }

    public static Map<String, Object> habit(String name, long categoryId, long subcategoryId) {
        Map<String, Object> body = habit(name);
        body.put("categoryId", categoryId);
        body.put("subcategoryId", subcategoryId);
        return body;
    }

    public static Map<String, Object> counter(String name) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("description", "Days without sugar");
        body.put("icon", "🍬");
        return body;
    }

    public static Map<String, Object> category(String name) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("icon", "🧪");
        return body;
    }

    public static Map<String, Object> subcategory(String name, Long categoryId) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("icon", "🔬");
        body.put("categoryId", categoryId);
        return body;
    }

    public static Map<String, Object> mediaItem(String videoId) {
        Map<String, Object> body = new HashMap<>();
        body.put("videoId", videoId);
        body.put("title", "Stoicism explained in 10 minutes");
        body.put("channelTitle", "Philosophy Channel");
        body.put("thumbnailUrl", "https://img.example.test/" + videoId + ".jpg");
        body.put("description", "An introduction to Stoic philosophy");
        body.put("matchedInterest", "Stoicism");
        return body;
    }
}
