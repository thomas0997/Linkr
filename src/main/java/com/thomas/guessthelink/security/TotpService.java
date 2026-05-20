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
    private static final String BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    public boolean verify(String base32Secret, String userCode) {
        if (base32Secret == null || userCode == null) return false;
        userCode = userCode.replaceAll("\\s", "");
        if (userCode.length() != 6) return false;

        try {
            byte[] secret = base32Decode(base32Secret);
            long window = Instant.now().getEpochSecond() / PERIOD;

            for (long w = window - 1; w <= window + 1; w++) {
                String expected = String.format("%06d", generateCode(secret, w));
                if (expected.equals(userCode)) return true;
            }
            return false;

        } catch (Exception e) {
            return false;
        }
    }

    private int generateCode(byte[] secret, long window) throws Exception {
        byte[] msg = ByteBuffer.allocate(8).putLong(window).array();
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

    public String generateSecret() {
        StringBuilder sb = new StringBuilder();
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < 32; i++) {
            sb.append(BASE32.charAt(random.nextInt(32)));
        }
        return sb.toString();
    }

    public String getQRCodeImageUrl(String secret, String account, String issuer) {
        String otpauth = "otpauth://totp/"
            + URLEncoder.encode(issuer + ":" + account, StandardCharsets.UTF_8)
            + "?secret=" + secret
            + "&issuer=" + URLEncoder.encode(issuer, StandardCharsets.UTF_8)
            + "&algorithm=SHA1&digits=6&period=30";

        return "https://api.qrserver.com/v1/create-qr-code/?size=220x220&data="
            + URLEncoder.encode(otpauth, StandardCharsets.UTF_8);
    }

    public byte[] base32Decode(String input) {
        input = input.toUpperCase().replaceAll("[^A-Z2-7]", "");
        byte[] out = new byte[input.length() * 5 / 8];
        int buffer = 0, bitsLeft = 0, idx = 0;
        for (char c : input.toCharArray()) {
            buffer = (buffer << 5) | BASE32.indexOf(c);
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                bitsLeft -= 8;
                out[idx++] = (byte) ((buffer >> bitsLeft) & 0xFF);
            }
        }
        return out;
    }
}