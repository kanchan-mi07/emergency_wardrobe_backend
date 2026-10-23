package com.example.emergencywardrobe.controller;

import com.example.emergencywardrobe.service.ImageSearchService;
import com.example.emergencywardrobe.service.ImageSearchService.HeroPhoto;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Public site-wide content. Currently: the home-page hero photograph.
 * The result is cached in memory so Unsplash is called at most once a day (and at most once
 * every 10 minutes if the lookup keeps failing). A 204 tells the frontend to use its fallback artwork.
 */
@RestController
@RequestMapping("/api/site")
public class SiteController {

    private static final String HERO_QUERY = "women clothing boutique rack dresses";
    private static final long SUCCESS_TTL_MS = TimeUnit.HOURS.toMillis(24);
    private static final long FAILURE_TTL_MS = TimeUnit.MINUTES.toMillis(10);

    private record Cached(Optional<HeroPhoto> photo, long at) { }

    private final ImageSearchService imageSearchService;
    private volatile Cached cached;

    public SiteController(ImageSearchService imageSearchService) {
        this.imageSearchService = imageSearchService;
    }

    @GetMapping("/hero")
    public ResponseEntity<Map<String, String>> hero() {
        Cached c = cached;
        long now = System.currentTimeMillis();
        long ttl = (c != null && c.photo().isPresent()) ? SUCCESS_TTL_MS : FAILURE_TTL_MS;

        if (c == null || now - c.at() > ttl) {
            c = new Cached(imageSearchService.findHeroPhoto(HERO_QUERY), now);
            cached = c;
        }

        if (c.photo().isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        HeroPhoto p = c.photo().get();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .body(Map.of("url", p.url(), "author", p.author(), "authorUrl", p.authorUrl()));
    }
}
