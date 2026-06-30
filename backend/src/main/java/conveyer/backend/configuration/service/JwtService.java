package conveyer.backend.configuration.service;

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

import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
public class JwtService {

  @Value("${JWT_SECRET}")
  private String secret;

  @Value("${JWT_ACCESS_TOKEN_EXPIRATION}")
  private Long accessTokenExpiration;

  @Value("${JWT_REFRESH_TOKEN_EXPIRATION}")
  private Long refreshTokenExpiration;

  private static final String REFRESH_TOKEN_PREFIX = "refresh:";

  private final StringRedisTemplate redisTemplate;




  public JwtService(StringRedisTemplate redisTemplate) {
  this.redisTemplate = redisTemplate;
}

  public String generateAccessToken(String subject) throws JOSEException {
    return generateToken(subject, accessTokenExpiration);
  }

  public String generateRefreshToken(String subject) throws JOSEException {
    return generateToken(subject, refreshTokenExpiration);
  }

  private String generateToken(String subject, Long expirationMillis) throws JOSEException {
    Instant now = Instant.now();
    Instant expiration = now.plusMillis(expirationMillis);

    // Create claims
    JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
        .subject(subject.toString())
        .issueTime(Date.from(now))
        .expirationTime(Date.from(expiration))
        .claim("token_type", expirationMillis == accessTokenExpiration ? "ACCESS" : "REFRESH")
        .build();

    JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);

    SignedJWT signedJWT = new SignedJWT(header, claimsSet);

    MACSigner signer = new MACSigner(secret.getBytes());
    signedJWT.sign(signer);
    return signedJWT.serialize();
  }

  /**
   * Validate a JWT token and extract username
   */
  public String validateAndExtractSubject(String token) throws JOSEException, ParseException {
    // Parse the token
    SignedJWT signedJWT = SignedJWT.parse(token);

    // Verify signature
    MACVerifier verifier = new MACVerifier(secret.getBytes());
    if (!signedJWT.verify(verifier)) {
      throw new JOSEException("Invalid JWT signature");
    }

    // Check expiration
    JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
    Date expirationTime = claimsSet.getExpirationTime();

    if (expirationTime != null && expirationTime.before(new Date())) {
      throw new JOSEException("JWT token has expired");
    }

    return claimsSet.getSubject();
  }

  public boolean validateToken(String token) {
    try {
      SignedJWT signedJWT = SignedJWT.parse(token);
      MACVerifier verifier = new MACVerifier(secret.getBytes());

      if (!signedJWT.verify(verifier)) {
        return false;
      }

      JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
      Date expirationTime = claimsSet.getExpirationTime();

      return expirationTime != null && expirationTime.after(new Date());

    } catch (Exception e) {
      return false;
    }
  }

  public String extractSubject(String token) throws ParseException {
    SignedJWT signedJWT = SignedJWT.parse(token);
    return signedJWT.getJWTClaimsSet().getSubject();
  }

  public String extractTokenType(String token) throws ParseException {
    SignedJWT signedJWT = SignedJWT.parse(token);
    return signedJWT.getJWTClaimsSet().getStringClaim("token_type");
  }

  public boolean isTokenExpired(String token) throws ParseException {
    SignedJWT signedJWT = SignedJWT.parse(token);
    JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
    Date expirationTime = claimsSet.getExpirationTime();
    return expirationTime != null && expirationTime.before(new Date());
  }

  public Date getExpirationDate(String token) throws ParseException {
    SignedJWT signedJWT = SignedJWT.parse(token);
    return signedJWT.getJWTClaimsSet().getExpirationTime();
  }


    public void saveRefreshToken(String email, String refreshToken) {
        String key = REFRESH_TOKEN_PREFIX + email;
        redisTemplate.opsForValue().set(key, refreshToken, refreshTokenExpiration, TimeUnit.MILLISECONDS);
    }

    public boolean validateRefreshToken(String email, String refreshToken) {
        String key = REFRESH_TOKEN_PREFIX + email;
        String storedToken = redisTemplate.opsForValue().get(key);
        return refreshToken.equals(storedToken);
    }

    public void deleteRefreshToken(String email) {
        String key = REFRESH_TOKEN_PREFIX + email;
        redisTemplate.delete(key);
    }
}
