package uk.gov.hmcts.marketplace.client.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Slf4j
@Service
public class HmacVerificationService {

    private static final String HMAC_SHA256 = "HmacSHA256";

    public boolean verify(String method, String uri, String body, String signature, String secret) {
        if (signature == null || secret == null) {
            return false;
        }
        String expected = sign(method, uri, body, secret);
        return java.security.MessageDigest.isEqual(
                Base64.getDecoder().decode(signature),
                Base64.getDecoder().decode(expected));
    }

    private String sign(String method, String uri, String body, String secret) {
        String payload = method + "\n" + uri + "\n" + (body != null ? body : "");
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] hmac = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hmac);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalArgumentException("HMAC verification failed", e);
        }
    }
}
