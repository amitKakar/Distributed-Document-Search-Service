package com.example.multitenantdocumentsearch.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implements per-tenant rate limiting using a sliding window counter.
 * Limits the number of requests per tenant per minute.
 */
@Service
public class RateLimiterService {
    // Rate limit configuration: max requests per minute per tenant
    private static final int MAX_REQUESTS_PER_MINUTE = 30;
    private static final long WINDOW_MILLIS = 60_000L;

    // Map: tenantId -> (windowStart, requestCount)
    private final Map<String, Window> tenantWindows = new ConcurrentHashMap<>();

    /**
     * Checks if the tenant is within the allowed rate limit.
     * @param tenantId The tenant identifier
     * @return true if allowed, false if rate limit exceeded
     */
    public synchronized boolean isAllowed(String tenantId) {
        long now = Instant.now().toEpochMilli();
        Window window = tenantWindows.computeIfAbsent(tenantId, k -> new Window(now, 0));
        if (now - window.windowStart >= WINDOW_MILLIS) {
            // Reset window
            window.windowStart = now;
            window.requestCount = 1;
            return true;
        } else {
            if (window.requestCount < MAX_REQUESTS_PER_MINUTE) {
                window.requestCount++;
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * Sliding window state for a tenant.
     */
    private static class Window {
        long windowStart;
        int requestCount;
        Window(long windowStart, int requestCount) {
            this.windowStart = windowStart;
            this.requestCount = requestCount;
        }
    }
}

