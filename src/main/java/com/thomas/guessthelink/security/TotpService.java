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

    // Changed from 30 seconds to 3600 seconds (1 hour)
    private static final long PERIOD = 3600;

    private int generateCode(byte[] secret, long timeWindow) throws Exception {
        byte[] msg = ByteBuffer.allocate(8).putLong(timeWindow).array();
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(secret, "HmacSHA1"));
        byte[] hash = mac.doFinal(msg);
        int offset = hash[hash.length - 1] & 0x0F;
        int code = ((hash[offset]     & 0x7F) << 24)
                | ((hash[offset + 1] & 0xFF) << 16)
                | ((hash[offset + 2] & 0xFF) << 8)
                |  (hash[offset + 3] & 0xFF);
        return code % 1_000_000;
    }

    public boolean verify(String base32Secret, String userCode) {
        if (base32Secret == null || userCode == null) return false;
        userCode = userCode.replaceAll("\\s", "");
        if (userCode.length() != 6) return false;

        try {
            byte[] secret = base32Decode(base32Secret);
            long window = Instant.now().getEpochSecond() / PERIOD;

            // Allow ±1 window (±1 hour) for clock drift
            for (long w = window - 1; w <= window + 1; w++) {
                int expected = generateCode(secret, w);
                System.out.println("[TOTP] window=" + w + " expected=" + String.format("%06d", expected));
                if (String.format("%06d", expected).equals(userCode)) return true;
            }
            return false;

        } catch (Exception e) {
            System.err.println("TOTP verify error: " + e.getMessage());
            return false;
        }
    }

    public String generateSecret() {
        byte[] bytes = new byte[20];
        new SecureRandom().nextBytes(bytes);
        return base32Encode(bytes);
    }

    public String getQRCodeImageUrl(String secret, String account, String issuer) {
        // period=3600 tells Google Authenticator to use 1-hour windows
        String otpauth = "otpauth://totp/"
            + issuer + ":" + account
            + "?secret=" + secret
            + "&issuer=" + URLEncoder.encode(issuer, StandardCharsets.UTF_8)
            + "&algorithm=SHA1&digits=6&period=3600";
        return "https://api.qrserver.com/v1/create-qr-code/?size=220x220&data="
            + URLEncoder.encode(otpauth, StandardCharsets.UTF_8);
    }

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