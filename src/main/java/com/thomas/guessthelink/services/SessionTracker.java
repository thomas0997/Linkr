package com.thomas.guessthelink.services;

import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionTracker {

    private static final long ACTIVE_SECONDS = 300; // 5 minutes

    private final Map<Long, Instant> lastSeen = new ConcurrentHashMap<>();

    public void markActive(Long playerId) {
        if (playerId != null) {
            lastSeen.put(playerId, Instant.now());
        }
    }

    public long getActiveCount() {
        Instant cutoff = Instant.now().minusSeconds(ACTIVE_SECONDS);
        lastSeen.entrySet().removeIf(e -> e.getValue().isBefore(cutoff));
        return lastSeen.size();
    }
}