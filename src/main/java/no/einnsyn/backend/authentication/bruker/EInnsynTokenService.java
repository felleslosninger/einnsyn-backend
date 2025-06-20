package no.einnsyn.backend.authentication.bruker;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import no.einnsyn.backend.entities.bruker.models.Bruker;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

@Service
public class EInnsynTokenService {

  private final JwtDecoder jwtDecoder;
  private final JwtEncoder jwtEncoder;
  @Getter private final long expiration;
  private final long refreshExpiration;
  private final String issuerUri;

  public EInnsynTokenService(
      @Qualifier("eInnsynJwtDecoder") JwtDecoder jwtDecoder,
      @Qualifier("eInnsynJwtEncoder") JwtEncoder jwtEncoder,
      @Value("${application.jwt.accessTokenExpiration}") long expiration,
      @Value("${application.jwt.refreshTokenExpiration}") long refreshExpiration,
      @Value("${application.jwt.issuerUri}") String issuerUri) {
    this.jwtDecoder = jwtDecoder;
    this.jwtEncoder = jwtEncoder;
    this.expiration = expiration;
    this.refreshExpiration = refreshExpiration;
    this.issuerUri = issuerUri;
  }

  public String generateToken(Bruker userDetails) {
    return generateToken(Map.of("use", "access"), userDetails, expiration);
  }

  public String generateRefreshToken(Bruker userDetails) {
    return generateToken(Map.of("use", "refresh"), userDetails, refreshExpiration);
  }

  public String generateToken(Map<String, Object> extraClaims, Bruker bruker, long expiration) {
    var now = Instant.now();
    var claimsBuilder =
        JwtClaimsSet.builder()
            .issuer(issuerUri)
            .issuedAt(now)
            .expiresAt(now.plus(expiration, ChronoUnit.SECONDS))
            .subject(bruker.getEmail())
            .id(UUID.randomUUID().toString())
            .claim("id", bruker.getId())
            .claims(claims -> claims.putAll(extraClaims));

    var claims = claimsBuilder.build();
    var header =
        JwsHeader.with(MacAlgorithm.HS256)
            .keyId(EInnsynJwtConfiguration.EINNSYN_JWT_KEY_ID)
            .type("JWT")
            .build();
    var encoderParameters = JwtEncoderParameters.from(header, claims);

    return jwtEncoder.encode(encoderParameters).getTokenValue();
  }

  public Jwt decodeToken(String token) throws JwtException {
    return jwtDecoder.decode(token);
  }
}
