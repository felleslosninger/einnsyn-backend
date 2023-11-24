package no.einnsyn.apiv3.authentication.bruker;

import java.util.Date;
import java.util.Map;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import no.einnsyn.apiv3.authentication.bruker.models.BrukerUserDetails;

@Service
public class JwtService {

  @Value("${application.jwt.secret}")
  private String secret;

  @Getter
  @Value("#{${application.jwt.accessTokenExpiration}}")
  private long expiration;

  @Value("#{${application.jwt.refreshTokenExpiration}}")
  private long refreshExpiration;


  public String getUsername(String token) {
    var claims = extractAllClaims(token);
    return claims.getSubject();
  }


  /**
   * Returns the username if the token is not expired and the use is correct.
   * 
   * @param token
   * @param use "access" for access tokens, "refresh" for refresh tokens
   * @return
   */
  public String validateAndReturnUsername(String token, String use) {
    try {
      var claims = extractAllClaims(token);
      var isExpired = claims.getExpiration().before(new Date());
      var isCorrectUse = claims.get("use").equals(use);
      if (isExpired || !isCorrectUse) {
        return null;
      }
      return claims.getSubject();
    } catch (Exception e) {
      return null;
    }
  }


  public Claims extractAllClaims(String token) {
    // @formatter:off
    return Jwts
      .parser()
      .verifyWith(getSecretKey())
      .build()
      .parseSignedClaims(token)
      .getPayload();
    // @formatter:on
  }


  public String generateToken(BrukerUserDetails userDetails) {
    return generateToken(Map.of("use", "access"), userDetails, expiration);
  }


  public String generateRefreshToken(BrukerUserDetails userDetails) {
    return generateToken(Map.of("use", "refresh"), userDetails, refreshExpiration);
  }


  public String generateToken(Map<String, Object> extraClaims, BrukerUserDetails userDetails,
      long expiration) {
    var secretKey = getSecretKey();

    // @formatter:off
    return Jwts
      .builder()
      .claims(extraClaims)
      .subject(userDetails.getUsername())
      .issuedAt(new Date(System.currentTimeMillis()))
      .expiration(new Date(System.currentTimeMillis() + (expiration * 1000)))
      .signWith(secretKey)
      .compact();
    // @formatter:on
  }


  public SecretKey getSecretKey() {
    byte[] secretBytes = Decoders.BASE64.decode(secret);
    return Keys.hmacShaKeyFor(secretBytes);
  }

}
