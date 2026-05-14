package com.thomas.guessthelink.security;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    // How it works:
    // We keep a small record per IP: how many failed attempts, and when the lockout expires.
    // ConcurrentHashMap is thread-safe so multiple requests at once won't corrupt state.
    // Nothing is stored in the DB — restarting the server resets all lockouts (fine for admin).

    private static final int    MAX_ATTEMPTS    = 5;
    private static final long   LOCKOUT_SECONDS = 30 * 60; // 30 minutes

    private record Attempt(int count, Instant lockedUntil) {}

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    /** Returns true if this IP is currently locked out. */
    public boolean isBlocked(String ip) {
        Attempt a = attempts.get(ip);
        if (a == null) return false;
        if (a.lockedUntil() != null && Instant.now().isBefore(a.lockedUntil())) return true;
        // Lockout expired — clean up
        if (a.lockedUntil() != null) attempts.remove(ip);
        return false;
    }

    /** Call this on every failed login attempt. */
    public void recordFailure(String ip) {
        Attempt current = attempts.getOrDefault(ip, new Attempt(0, null));
        int newCount = current.count() + 1;
        Instant lockUntil = newCount >= MAX_ATTEMPTS
            ? Instant.now().plusSeconds(LOCKOUT_SECONDS)
            : null;
        attempts.put(ip, new Attempt(newCount, lockUntil));
    }

    /** Call this on successful login to reset the counter. */
    public void recordSuccess(String ip) {
        attempts.remove(ip);
    }

    /** How many minutes remain in the lockout (for showing to the user). */
    public long minutesRemaining(String ip) {
        Attempt a = attempts.get(ip);
        if (a == null || a.lockedUntil() == null) return 0;
        long secs = Instant.now().until(a.lockedUntil(), java.time.temporal.ChronoUnit.SECONDS);
        return Math.max(0, (secs + 59) / 60); // round up
    }
}