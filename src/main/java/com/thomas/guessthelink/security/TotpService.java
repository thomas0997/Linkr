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

    private static final long PERIOD = 30;
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    public boolean verify(String base32Secret, String userCode) {
        if (base32Secret == null || userCode == null) return false;
        userCode = userCode.replaceAll("\\s", "");
        if (userCode.length() != 6) return false;

        try {
            byte[] secret = base32Decode(base32Secret);
            long window = Instant.now().getEpochSecond() / PERIOD;

            // Print server time for debugging — compare to `date` in terminal
            System.out.println("[TOTP] Server epoch: " + Instant.now().getEpochSecond());
            System.out.println("[TOTP] Current window: " + window);

            for (long w = window - 2; w <= window + 2; w++) {
                String expected = String.format("%06d", hotp(secret, w));
                System.out.println("[TOTP] window=" + w + " expected=" + expected + " got=" + userCode);
                if (expected.equals(userCode)) return true;
            }
            return false;
        } catch (Exception e) {
            System.err.println("[TOTP] Error: " + e.getMessage());
            return false;
        }
    }

    private int hotp(byte[] secret, long counter) throws Exception {
        byte[] msg = ByteBuffer.allocate(8).putLong(counter).array();
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(secret, "HmacSHA1"));
        byte[] h = mac.doFinal(msg);
        int offset = h[19] & 0x0F;
        int code = ((h[offset]     & 0x7F) << 24)
                 | ((h[offset + 1] & 0xFF) << 16)
                 | ((h[offset + 2] & 0xFF) << 8)
                 |  (h[offset + 3] & 0xFF);
        return code % 1_000_000;
    }

    public String generateSecret() {
        SecureRandom rng = new SecureRandom();
        StringBuilder sb = new StringBuilder(32);
        for (int i = 0; i < 32; i++) sb.append(ALPHABET.charAt(rng.nextInt(32)));
        return sb.toString();
    }

    public String getQRCodeImageUrl(String secret, String account, String issuer) {
        String label = URLEncoder.encode(issuer + ":" + account, StandardCharsets.UTF_8);
        String iss   = URLEncoder.encode(issuer, StandardCharsets.UTF_8);
        String otpauth = "otpauth://totp/" + label
                       + "?secret=" + secret
                       + "&issuer=" + iss
                       + "&algorithm=SHA1&digits=6&period=30";
        System.out.println("[TOTP] otpauth: " + otpauth);
        return "https://api.qrserver.com/v1/create-qr-code/?size=220x220&data="
               + URLEncoder.encode(otpauth, StandardCharsets.UTF_8);
    }

    public byte[] base32Decode(String input) {
        input = input.toUpperCase().replaceAll("[^A-Z2-7]", "");
        byte[] out = new byte[input.length() * 5 / 8];
        int buffer = 0, bitsLeft = 0, idx = 0;
        for (char c : input.toCharArray()) {
            buffer = (buffer << 5) | ALPHABET.indexOf(c);
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                bitsLeft -= 8;
                out[idx++] = (byte) ((buffer >> bitsLeft) & 0xFF);
            }
        }
        System.out.println("[TOTP] Decoded secret length: " + out.length + " bytes (expected 20)");
        return out;
    }
}