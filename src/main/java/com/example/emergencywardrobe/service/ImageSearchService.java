package com.example.emergencywardrobe.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Slf4j
@Service
public class ImageSearchService {

    private final RestClient restClient = RestClient.builder()
            .baseUrl("https://api.unsplash.com")
            .build();

    @Value("${unsplash.access-key:}")
    private String accessKey;

    public Optional<String> findImageUrl(String productName) {
        if (accessKey == null || accessKey.isBlank() || productName == null || productName.isBlank()) {
            return Optional.empty();
        }
        try {
            String query = productName.trim() + " women fashion";
            JsonNode body = restClient.get()
                    .uri(u -> u.path("/search/photos")
                            .queryParam("query", "{q}")
                            .queryParam("per_page", 1)
                            .queryParam("orientation", "portrait")
                            .build(query))
                    .header("Authorization", "Client-ID " + accessKey)
                    .header("Accept-Version", "v1")
                    .retrieve()
                    .body(JsonNode.class);

            JsonNode results = body == null ? null : body.path("results");
            if (results != null && results.isArray() && results.size() > 0) {
                String url = results.get(0).path("urls").path("regular").asText("");
                if (!url.isBlank()) {
                    return Optional.of(url);
                }
            }
        } catch (Exception e) {
            log.warn("Image search failed for '{}': {}", productName, e.getMessage());
        }
        return Optional.empty();
    }
}