package com.thomas.guessthelink.security;


import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;

@Service
public class TotpService {

    // ── Core TOTP (RFC 6238) — no external library needed ────────────────────
    //
    // How it works:
    //  1. Convert the current time into a 30-second "window" number
    //  2. HMAC-SHA1(secret_bytes, window_as_8_bytes)  → 20-byte hash
    //  3. Pick 4 bytes from the hash (dynamic truncation) → 6-digit number
    //
    // Your phone and the server both do this math with the same secret → same code.

    private int generateCode(byte[] secret, long timeWindow) throws Exception {
        // Pack the window counter into 8 bytes (big-endian)
        byte[] msg = ByteBuffer.allocate(8).putLong(timeWindow).array();

        // HMAC-SHA1
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(secret, "HmacSHA1"));
        byte[] hash = mac.doFinal(msg);

        // Dynamic truncation: low 4 bits of last byte = offset
        int offset = hash[hash.length - 1] & 0x0F;

        // Read 4 bytes from that offset, mask the sign bit, mod 1_000_000 → 6 digits

        int code = ((hash[offset]     & 0x7F) << 24)
                | ((hash[offset + 1] & 0xFF) << 16)
                | ((hash[offset + 2] & 0xFF) << 8)
                | (hash[offset + 3] & 0xFF);

        return code % 1_000_000;
    }

    /**
     * Verifies a 6-digit code. Accepts current window ± 1 to handle clock drift.
     */
    public boolean verify(String base32Secret, String userCode) {
        if (base32Secret == null || userCode == null) return false;
        userCode = userCode.replaceAll("\\s", ""); // strip accidental spaces
        if (userCode.length() != 6) return false;

        try {
            byte[] secret = base32Decode(base32Secret);
            long window = Instant.now().getEpochSecond() / 30;

            // Check current window and ±1 either side (handles up to 30s clock drift)
            for (long w = window - 1; w <= window + 1; w++) {
                int expected = generateCode(secret, w);
                if (String.format("%06d", expected).equals(userCode)) return true;
            }
            return false;

        } catch (Exception e) {
            System.err.println("TOTP verify error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Generates a random Base32 secret key. Call once during /admin/setup.
     */
    public String generateSecret() {
        byte[] bytes = new byte[20];
        new SecureRandom().nextBytes(bytes);
        return base32Encode(bytes);
    }

    /**
     * Returns a Google Charts URL that renders the QR code as a scannable PNG.
     * Use as <img th:src="${qrUrl}"> on the setup page.
     */
    public String getQRCodeImageUrl(String secret, String account, String issuer) {
        String otpauth = "otpauth://totp/"
            + URLEncoder.encode(issuer + ":" + account, StandardCharsets.UTF_8)
            + "?secret=" + secret
            + "&issuer=" + URLEncoder.encode(issuer, StandardCharsets.UTF_8)
            + "&algorithm=SHA1&digits=6&period=30";
        return "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data="
            + URLEncoder.encode(otpauth, StandardCharsets.UTF_8);
    }

    // ── Base32 (RFC 4648) ────────────────────────────────────────────────────
    // Google Authenticator uses Base32, not Base64.

    private static final String B32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private String base32Encode(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int buffer = 0, bitsLeft = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                bitsLeft -= 5;
                sb.append(B32.charAt((buffer >> bitsLeft) & 31));
            }
        }
        if (bitsLeft > 0) sb.append(B32.charAt((buffer << (5 - bitsLeft)) & 31));
        return sb.toString();
    }

    public byte[] base32Decode(String input) {
        input = input.toUpperCase().replaceAll("[^A-Z2-7]", "");
        byte[] out = new byte[input.length() * 5 / 8];
        int buffer = 0, bitsLeft = 0, idx = 0;
        for (char c : input.toCharArray()) {
            buffer = (buffer << 5) | B32.indexOf(c);
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                bitsLeft -= 8;
                out[idx++] = (byte) ((buffer >> bitsLeft) & 0xFF);
            }
        }
        return out;
    }
}