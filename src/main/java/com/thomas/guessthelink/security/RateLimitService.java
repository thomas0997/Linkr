package com.thomas.guessthelink.security;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

// Tracks failed admin login attempts per IP.
// After 5 failures, the IP is locked out for 30 minutes.
@Service
public class RateLimitService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCKOUT_SECONDS = 30 * 60; // 30 minutes

    private final ConcurrentHashMap<String, Integer> attempts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Instant> lockedUntil = new ConcurrentHashMap<>();

    public boolean isLocked(String ip) {
        Instant until = lockedUntil.get(ip);
        if (until == null) return false;
        if (Instant.now().isAfter(until)) {
            // Lockout expired — clear it
            lockedUntil.remove(ip);
            attempts.remove(ip);
            return false;
        }
        return true;
    }

    public void recordFailure(String ip) {
        int count = attempts.merge(ip, 1, Integer::sum);
        if (count >= MAX_ATTEMPTS) {
            lockedUntil.put(ip, Instant.now().plusSeconds(LOCKOUT_SECONDS));
        }
    }

    public void reset(String ip) {
        attempts.remove(ip);
        lockedUntil.remove(ip);
    }

    // Returns minutes remaining in the lockout (for showing in the error message)
    public long getMinutesRemaining(String ip) {
        Instant until = lockedUntil.get(ip);
        if (until == null) return 0;
        long seconds = until.getEpochSecond() - Instant.now().getEpochSecond();
        return Math.max(0, (long) Math.ceil(seconds / 60.0));
    }

    public int getAttemptCount(String ip) {
        return attempts.getOrDefault(ip, 0);
    }
}