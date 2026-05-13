package com.thomas.guessthelink.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.time.Instant;

// Manual TOTP implementation (RFC 6238) — no library uncertainty.
// Secret is stored as Base32 in application.properties (standard for authenticator apps).
@Service
public class TotpService {

    private static final String BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final int WINDOW = 1; // allow ±1 time step for clock skew

    @Value("${admin.totp.secret:}")
    private String storedSecret;

    // True if a secret has been configured — setup page checks this
    public boolean isConfigured() {
        return storedSecret != null && !storedSecret.isBlank();
    }

    // Generate a fresh 20-byte random secret, returned as Base32
    public String generateBase32Secret() {
        byte[] bytes = new byte[20];
        new java.security.SecureRandom().nextBytes(bytes);
        return encodeBase32(bytes);
    }

    // Build the otpauth:// URI that Google Authenticator reads from the QR code
    public String getOtpAuthUri(String base32Secret) {
        return "otpauth://totp/Linkr%20Admin:admin"
            + "?secret=" + base32Secret
            + "&issuer=Linkr%20Admin"
            + "&algorithm=SHA1"
            + "&digits=6"
            + "&period=30";
    }

    // Verify a 6-digit code against the stored secret
    public boolean verify(String code) {
        if (!isConfigured()) return false;
        return verifyWithSecret(code, storedSecret);
    }

    // Verify against an explicit secret (used during setup before it's saved)
    public boolean verifyWithSecret(String code, String base32Secret) {
        try {
            byte[] secret = decodeBase32(base32Secret);
            long counter = Instant.now().getEpochSecond() / 30;
            for (int i = -WINDOW; i <= WINDOW; i++) {
                String expected = String.format("%06d", hotp(secret, counter + i));
                if (expected.equals(code.trim())) return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    // HOTP algorithm (RFC 4226) — TOTP calls this with time-based counter
    private int hotp(byte[] secret, long counter) throws Exception {
        byte[] msg = ByteBuffer.allocate(8).putLong(counter).array();
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(secret, "HmacSHA1"));
        byte[] hash = mac.doFinal(msg);
        int offset = hash[hash.length - 1] & 0x0f;
        int binary = ((hash[offset]     & 0x7f) << 24)
                   | ((hash[offset + 1] & 0xff) << 16)
                   | ((hash[offset + 2] & 0xff) << 8)
                   |  (hash[offset + 3] & 0xff);
        return binary % 1_000_000;
    }

    // Base32 encode (RFC 4648) — needed for the otpauth:// URI
    public String encodeBase32(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int buffer = 0, bitsLeft = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                bitsLeft -= 5;
                sb.append(BASE32_CHARS.charAt((buffer >> bitsLeft) & 0x1F));
            }
        }
        if (bitsLeft > 0) {
            buffer <<= (5 - bitsLeft);
            sb.append(BASE32_CHARS.charAt(buffer & 0x1F));
        }
        return sb.toString();
    }

    // Base32 decode — used when reading secret from application.properties
    public byte[] decodeBase32(String data) {
        data = data.toUpperCase().replaceAll("[^A-Z2-7]", "");
        byte[] result = new byte[data.length() * 5 / 8];
        int buffer = 0, bitsLeft = 0, index = 0;
        for (char c : data.toCharArray()) {
            buffer = (buffer << 5) | BASE32_CHARS.indexOf(c);
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                bitsLeft -= 8;
                if (index < result.length) result[index++] = (byte) ((buffer >> bitsLeft) & 0xFF);
            }
        }
        return result;
    }
}