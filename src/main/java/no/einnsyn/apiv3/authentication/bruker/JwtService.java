package no.einnsyn.apiv3.authentication.bruker;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.Getter;
import no.einnsyn.apiv3.authentication.bruker.models.BrukerUserDetails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  @Value("${application.jwt.encryption-secret}")
  private String secret;

  @Getter
  @Value("${application.jwt.accessTokenExpiration}")
  private long expiration;

  @Value("${application.jwt.refreshTokenExpiration}")
  private long refreshExpiration;

  private SecretKey secretKey;

  public String getUsername(String token) {
    var claims = extractAllClaims(token);
    return claims.getSubject();
  }

  /**
   * Returns the username if the token is not expired and the use is correct.
   *
   * @param token The access or refresh token
   * @param use "access" for access tokens, "refresh" for refresh tokens
   * @return Username if all is well, else null.
   */
  public String validateAndReturnIdOrUsername(String token, String use) {
    try {
      var claims = extractAllClaims(token);
      var isExpired = claims.getExpiration().before(new Date());
      var isCorrectUse = claims.get("use").equals(use);
      if (isExpired || !isCorrectUse) {
        return null;
      }

      // We prefer ID, since in the odd case that the user changes their username (email), the ID
      // will still be the same. The old API doesn't store IDs in the token, so we need a fallback.
      if (claims.get("id") != null) {
        return claims.get("id").toString();
      }
      return claims.getSubject();
    } catch (Exception e) {
      return null;
    }
  }

  public Claims extractAllClaims(String token) {
    return Jwts.parser().verifyWith(getSecretKey()).build().parseSignedClaims(token).getPayload();
  }

  public String generateToken(BrukerUserDetails userDetails) {
    return generateToken(Map.of("use", "access"), userDetails, expiration);
  }

  public String generateRefreshToken(BrukerUserDetails userDetails) {
    return generateToken(Map.of("use", "refresh"), userDetails, refreshExpiration);
  }

  public String generateToken(
      Map<String, Object> extraClaims, BrukerUserDetails userDetails, long expiration) {

    return Jwts.builder()
        .claims(extraClaims)
        .claim("jti", UUID.randomUUID().toString())
        .claim("id", userDetails.getId())
        .subject(userDetails.getUsername())
        .issuedAt(new Date(System.currentTimeMillis()))
        .expiration(new Date(System.currentTimeMillis() + (expiration * 1000)))
        .signWith(getSecretKey())
        .compact();
  }

  public SecretKey getSecretKey() {
    if (secretKey == null) {
      byte[] secretBytes = Base64.getDecoder().decode(secret);
      secretKey = Keys.hmacShaKeyFor(secretBytes);
    }
    return secretKey;
  }
}
