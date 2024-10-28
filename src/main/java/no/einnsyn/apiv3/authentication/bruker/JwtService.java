package no.einnsyn.apiv3.authentication.bruker;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.io.IOException;
import java.io.StringReader;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.Getter;
import no.einnsyn.apiv3.authentication.bruker.models.BrukerUserDetails;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  @Value("${application.jwt.encryption-secret}")
  private String secret;

  @Value("${application.jwt.isRSA}")
  private boolean keyIsRSA;

  @Getter
  @Value("${application.jwt.accessTokenExpiration}")
  private long expiration;

  @Value("${application.jwt.refreshTokenExpiration}")
  private long refreshExpiration;

  private KeyPair keyPair;

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
    if (keyIsRSA) {
      return Jwts.parser()
          .verifyWith(getKeyPair().getPublic())
          .build()
          .parseSignedClaims(token)
          .getPayload();
    } else {
      return Jwts.parser().verifyWith(getSecretKey()).build().parseSignedClaims(token).getPayload();
    }
  }

  public String generateToken(BrukerUserDetails userDetails) {
    return generateToken(Map.of("use", "access"), userDetails, expiration);
  }

  public String generateRefreshToken(BrukerUserDetails userDetails) {
    return generateToken(Map.of("use", "refresh"), userDetails, refreshExpiration);
  }

  public String generateToken(
      Map<String, Object> extraClaims, BrukerUserDetails userDetails, long expiration) {
    var secretKey = keyIsRSA ? getKeyPair().getPrivate() : getSecretKey();

    return Jwts.builder()
        .claims(extraClaims)
        .claim("jti", UUID.randomUUID().toString())
        .subject(userDetails.getUsername())
        .issuedAt(new Date(System.currentTimeMillis()))
        .expiration(new Date(System.currentTimeMillis() + (expiration * 1000)))
        .signWith(secretKey)
        .compact();
  }

  public SecretKey getSecretKey() {
    byte[] secretBytes = Decoders.BASE64.decode(secret);
    return Keys.hmacShaKeyFor(secretBytes);
  }

  private KeyPair getKeyPair() {
    if (keyPair == null) {
      try {
        var pemParser = new PEMParser(new StringReader(secret.replace("\\n", "\n")));
        var secretKey = (PEMKeyPair) pemParser.readObject();
        pemParser.close();

        var privateSpec =
            (RSAKeyParameters) PrivateKeyFactory.createKey(secretKey.getPrivateKeyInfo());
        var publicSpec =
            (RSAKeyParameters) PublicKeyFactory.createKey(secretKey.getPublicKeyInfo());
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PrivateKey privateKey =
            kf.generatePrivate(
                new RSAPrivateKeySpec(privateSpec.getModulus(), privateSpec.getExponent()));
        PublicKey publicKey =
            kf.generatePublic(
                new RSAPublicKeySpec(publicSpec.getModulus(), publicSpec.getExponent()));

        keyPair = new KeyPair(publicKey, privateKey);
      } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
        throw new RuntimeException(e);
      }
    }

    return keyPair;
  }
}
