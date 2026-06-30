package conveyer.backend.configuration.service;

import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

@Service
public class ApiKeyService {

  private static final String API_KEY_PREFIX = "apikey:";
  private static final String KEY_PREFIX = "sk-";

  @Value("${API_KEY_SECRET}")
  private String apiKeySecret;

  private final StringRedisTemplate redisTemplate;

  public ApiKeyService(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  public String generateApiKey(UUID userId, UUID projectId, Long expiresInSeconds) {
    String redisKey = API_KEY_PREFIX + userId + ":" + projectId;

    try {
      String apiKey = generateApiKeyToken(userId, projectId, expiresInSeconds);

      if (expiresInSeconds != null && expiresInSeconds > 0) {
        redisTemplate.opsForValue().set(redisKey, apiKey, expiresInSeconds, TimeUnit.SECONDS);
      } else {
        redisTemplate.opsForValue().set(redisKey, apiKey);
      }

      return apiKey;
    } catch (JOSEException e) {
      throw new RuntimeException("Failed to generate API key", e);
    }
  }

  private String generateApiKeyToken(UUID userId, UUID projectId, Long expiresInSeconds) throws JOSEException {
    Instant now = Instant.now();

    JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
        .subject(userId.toString())
        .issueTime(Date.from(now))
        .claim("project_id", projectId.toString())
        .claim("token_type", "API_KEY");

    if (expiresInSeconds != null && expiresInSeconds > 0) {
      claimsBuilder.expirationTime(Date.from(now.plusSeconds(expiresInSeconds)));
    }

    SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsBuilder.build());
    signedJWT.sign(new MACSigner(apiKeySecret.getBytes()));
    return KEY_PREFIX + signedJWT.serialize();
  }

  public String getApiKey(UUID userId, UUID projectId) {
    String redisKey = API_KEY_PREFIX + userId + ":" + projectId;
    return redisTemplate.opsForValue().get(redisKey);
  }

  public void deleteApiKey(UUID userId, UUID projectId) {
    String redisKey = API_KEY_PREFIX + userId + ":" + projectId;
    redisTemplate.delete(redisKey);
  }

  public String extractUserId(String apiKey) throws ParseException, JOSEException {
    SignedJWT signedJWT = parseAndVerify(apiKey);
    return signedJWT.getJWTClaimsSet().getSubject();
  }

  public String extractProjectId(String apiKey) throws ParseException, JOSEException {
    SignedJWT signedJWT = parseAndVerify(apiKey);
    return signedJWT.getJWTClaimsSet().getStringClaim("project_id");
  }

  private SignedJWT parseAndVerify(String apiKey) throws ParseException, JOSEException {
    if (apiKey == null || !apiKey.startsWith(KEY_PREFIX)) {
      throw new ParseException("Invalid API key format", 0);
    }

    String jwtString = apiKey.substring(KEY_PREFIX.length());
    SignedJWT signedJWT = SignedJWT.parse(jwtString);

    MACVerifier verifier = new MACVerifier(apiKeySecret.getBytes());
    if (!signedJWT.verify(verifier)) {
      throw new JOSEException("Invalid API key signature");
    }

    return signedJWT;
  }
}
