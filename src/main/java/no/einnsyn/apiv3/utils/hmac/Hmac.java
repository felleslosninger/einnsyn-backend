package no.einnsyn.apiv3.utils.hmac;

import java.math.BigInteger;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class Hmac {

  private Hmac() {}

  public static String generateHmac(String method, String path, String timestamp, String secret) {
    var message = method.toUpperCase() + "\n" + path + "\n" + timestamp;
    var secretKeySpec = new SecretKeySpec(secret.getBytes(), "SHA256");
    Mac mac = null;
    try {
      mac = Mac.getInstance("HmacSHA256");
      mac.init(secretKeySpec);
      return new BigInteger(1, mac.doFinal(message.getBytes())).toString(16);
    } catch (Exception e) {
      throw new RuntimeException("Could not create HMAC signature");
    }
  }
}
