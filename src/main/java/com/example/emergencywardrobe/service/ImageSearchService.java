package com.example.emergencywardrobe.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Finds a photo that actually matches a product, using the Unsplash search API.
 *
 * Two things make results relevant instead of random:
 *  1. The search query is built from a curated keyword (or the product name + a category hint),
 *     not just "<name> women fashion".
 *  2. We fetch several candidates and pick the one whose description/tags best match the query
 *     words, instead of blindly taking result #1.
 */
@Slf4j
@Service
public class ImageSearchService {

    /** Hand-tuned queries for the demo catalogue (keys are lower-cased product names). */
    private static final Map<String, String> CURATED_QUERIES = Map.ofEntries(
            Map.entry("period emergency kit", "sanitary pads feminine hygiene products"),
            Map.entry("wardrobe rescue kit", "sewing kit needle thread safety pins"),
            Map.entry("event emergency kit", "fashion tape safety pins sewing kit"),
            Map.entry("travel emergency kit", "travel toiletry pouch cosmetic bag"),
            Map.entry("beauty emergency kit", "makeup pouch cosmetics lip balm"),
            Map.entry("black evening dress", "black evening dress woman"),
            Map.entry("red party gown", "red gown dress woman"),
            Map.entry("floral summer dress", "floral summer dress woman"),
            Map.entry("embroidered kurti set", "embroidered kurti indian ethnic wear woman"),
            Map.entry("classic black heels", "black block heels shoes"),
            Map.entry("strappy sandals", "strappy sandals women"),
            Map.entry("casual flats", "ballet flats women shoes"),
            Map.entry("statement clutch bag", "evening clutch bag"),
            Map.entry("pearl jewelry set", "pearl necklace earrings jewelry")
    );

    /** Used when a product has no curated query: appended to the product name. */
    private static final Map<String, String> CATEGORY_HINTS = Map.of(
            "emergency kits", "kit pouch",
            "dresses", "dress woman",
            "kurtis", "kurti ethnic wear woman",
            "footwear", "shoes women",
            "accessories", "accessory women"
    );

    private final RestClient restClient = RestClient.builder()
            .baseUrl("https://api.unsplash.com")
            .build();

    @Value("${unsplash.access-key:}")
    private String accessKey;

    public boolean isConfigured() {
        return accessKey != null && !accessKey.isBlank();
    }

    /** True for missing images and for the random picsum.photos placeholders used by older seed data. */
    public static boolean isPlaceholder(String url) {
        return url == null || url.isBlank() || url.contains("picsum.photos");
    }

    public Optional<String> findImageUrl(String productName) {
        return findImageUrl(productName, null);
    }

    public Optional<String> findImageUrl(String productName, String categoryName) {
        if (!isConfigured() || productName == null || productName.isBlank()) {
            return Optional.empty();
        }
        String query = buildQuery(productName, categoryName);
        try {
            JsonNode body = restClient.get()
                    .uri(u -> u.path("/search/photos")
                            .queryParam("query", "{q}")
                            .queryParam("per_page", 10)
                            .queryParam("content_filter", "high")
                            .build(query))
                    .header("Authorization", "Client-ID " + accessKey)
                    .header("Accept-Version", "v1")
                    .retrieve()
                    .body(JsonNode.class);

            JsonNode results = body == null ? null : body.path("results");
            if (results == null || !results.isArray() || results.isEmpty()) {
                log.info("No image results for '{}' (query: '{}')", productName, query);
                return Optional.empty();
            }

            List<String> words = tokens(query);
            JsonNode best = null;
            int bestScore = -1;
            for (JsonNode photo : results) {            // results are already ordered by relevance
                int score = relevance(photo, words);
                if (score > bestScore) {                // strict ">" keeps the earlier (more relevant) one on ties
                    bestScore = score;
                    best = photo;
                }
            }

            String url = best == null ? "" : best.path("urls").path("regular").asText("");
            if (!url.isBlank()) {
                log.info("Image for '{}' chosen with query '{}' (match score {})", productName, query, bestScore);
                return Optional.of(url);
            }
        } catch (Exception e) {
            log.warn("Image search failed for '{}': {}", productName, e.getMessage());
        }
        return Optional.empty();
    }

    /** A landscape photo for the home-page hero, with the photographer credit Unsplash requires. */
    public record HeroPhoto(String url, String author, String authorUrl) { }

    public Optional<HeroPhoto> findHeroPhoto(String query) {
        if (!isConfigured()) {
            return Optional.empty();
        }
        try {
            JsonNode body = restClient.get()
                    .uri(u -> u.path("/search/photos")
                            .queryParam("query", "{q}")
                            .queryParam("per_page", 12)
                            .queryParam("orientation", "landscape")
                            .queryParam("content_filter", "high")
                            .build(query))
                    .header("Authorization", "Client-ID " + accessKey)
                    .header("Accept-Version", "v1")
                    .retrieve()
                    .body(JsonNode.class);

            JsonNode results = body == null ? null : body.path("results");
            if (results == null || !results.isArray() || results.isEmpty()) {
                return Optional.empty();
            }

            List<String> words = tokens(query);
            JsonNode best = null;
            int bestScore = -1;
            for (JsonNode photo : results) {
                // Prefer photos that match the query, then large ones (they stay sharp full-screen).
                int score = relevance(photo, words) * 10 + (photo.path("width").asInt(0) >= 3000 ? 1 : 0);
                if (score > bestScore) {
                    bestScore = score;
                    best = photo;
                }
            }

            String raw = best == null ? "" : best.path("urls").path("raw").asText("");
            if (raw.isBlank()) {
                return Optional.empty();
            }
            String url = raw + (raw.contains("?") ? "&" : "?") + "w=2000&q=75&fit=crop&auto=format";
            JsonNode user = best.path("user");
            String link = user.path("links").path("html").asText("");
            if (!link.isBlank()) {
                link += "?utm_source=emergency_wardrobe&utm_medium=referral";
            }
            return Optional.of(new HeroPhoto(url, user.path("name").asText(""), link));
        } catch (Exception e) {
            log.warn("Hero photo lookup failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    static String buildQuery(String productName, String categoryName) {
        String name = productName.trim().toLowerCase(Locale.ROOT);
        String curated = CURATED_QUERIES.get(name);
        if (curated != null) {
            return curated;
        }
        String hint = categoryName == null ? "" : CATEGORY_HINTS.getOrDefault(categoryName.trim().toLowerCase(Locale.ROOT), "");
        return (name + " " + hint).trim();
    }

    private static int relevance(JsonNode photo, List<String> words) {
        StringBuilder text = new StringBuilder();
        text.append(photo.path("alt_description").asText("")).append(' ');
        text.append(photo.path("description").asText("")).append(' ');
        JsonNode tags = photo.path("tags");
        if (tags.isArray()) {
            for (JsonNode tag : tags) {
                text.append(tag.path("title").asText("")).append(' ');
            }
        }
        String haystack = text.toString().toLowerCase(Locale.ROOT);
        int score = 0;
        for (String w : words) {
            if (haystack.contains(w)) {
                score++;
            }
        }
        return score;
    }

    private static List<String> tokens(String query) {
        List<String> out = new ArrayList<>();
        for (String t : query.toLowerCase(Locale.ROOT).split("\\s+")) {
            if (t.length() > 2) {
                out.add(t);
            }
        }
        return out;
    }
}