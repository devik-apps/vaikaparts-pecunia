package com.devikapps.vaikaparts.service.papi;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class PapiSignatureVerifier {
  private static final long TOLERANCE_SECONDS = 300;

  public boolean isValid(byte[] rawBody, String signatureHeader, String secret, Instant now) {
    if (rawBody == null || signatureHeader == null || secret == null || secret.isBlank()) {
      return false;
    }

    Map<String, String> parts = parseHeader(signatureHeader);
    String timestamp = parts.get("t");
    String signature = parts.get("v1");
    if (timestamp == null
        || !timestamp.matches("[0-9]{1,18}")
        || signature == null
        || !signature.matches("[0-9a-f]{64}")) {
      return false;
    }

    long timestampSeconds;
    try {
      timestampSeconds = Long.parseLong(timestamp);
    } catch (NumberFormatException e) {
      return false;
    }
    if (Math.abs(now.getEpochSecond() - timestampSeconds) > TOLERANCE_SECONDS) return false;

    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      mac.update((timestamp + ".").getBytes(StandardCharsets.UTF_8));
      byte[] expected = mac.doFinal(rawBody);
      return MessageDigest.isEqual(expected, HexFormat.of().parseHex(signature));
    } catch (Exception e) {
      return false;
    }
  }

  private Map<String, String> parseHeader(String signatureHeader) {
    Map<String, String> parts = new HashMap<>();
    for (String part : signatureHeader.split(",")) {
      String[] pair = part.trim().split("=", 2);
      if (pair.length == 2) parts.put(pair[0], pair[1]);
    }
    return parts;
  }
}
