package no.einnsyn.apiv3.utils.idgenerator;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.NoArgGenerator;
import com.fasterxml.uuid.impl.RandomBasedGenerator;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;
import java.util.UUID;
import no.einnsyn.apiv3.entities.base.models.Base;

public class IdGenerator {

  private IdGenerator() {}

  private static final char[] alphabet = "0123456789abcdefghjkmnpqrstvwxyz".toCharArray();
  private static final int CHAR_LENGTH = 26;
  private static final RandomBasedGenerator v4Generator = Generators.randomBasedGenerator();
  private static final TimeBasedEpochGenerator v7Generator = Generators.timeBasedEpochGenerator();

  public static String getPrefix(Class<? extends Base> clazz) {
    var className = clazz.getSimpleName().toLowerCase();
    return IdPrefix.map.getOrDefault(className, className);
  }

  public static String generateId(Class<? extends Base> clazz) {
    String className = clazz.getSimpleName().toLowerCase();
    return generateId(className);
  }

  public static String generateId(String entity) {
    entity = IdPrefix.map.getOrDefault(entity, entity);
    return entity + "_" + getRandomId();
  }

  public static String generateSecret(String prefix) {
    return prefix
        + "_"
        + getRandomId(v4Generator, CHAR_LENGTH)
        + getRandomId(v4Generator, CHAR_LENGTH);
  }

  private static String getRandomId() {
    return getRandomId(v7Generator, CHAR_LENGTH);
  }

  // From https://github.com/fxlae/typeid-java/
  // Original code is licensed under the Apache License, Version 2.0.
  private static String getRandomId(NoArgGenerator generator, int length) {
    final UUID uuid = generator.generate();
    final char[] chars = new char[length];
    long msb = uuid.getMostSignificantBits();
    long lsb = uuid.getLeastSignificantBits();

    // encode the MSBs except the last bit, as the block it belongs to overlaps with the LSBs
    chars[0x00] = alphabet[((int) (msb >>> 61) & 0x1F)];
    chars[0x01] = alphabet[(int) (msb >>> 56) & 0x1F];
    chars[0x02] = alphabet[(int) (msb >>> 51) & 0x1F];
    chars[0x03] = alphabet[(int) (msb >>> 46) & 0x1F];
    chars[0x04] = alphabet[(int) (msb >>> 41) & 0x1F];
    chars[0x05] = alphabet[(int) (msb >>> 36) & 0x1F];
    chars[0x06] = alphabet[(int) (msb >>> 31) & 0x1F];
    chars[0x07] = alphabet[(int) (msb >>> 26) & 0x1F];
    chars[0x08] = alphabet[(int) (msb >>> 21) & 0x1F];
    chars[0x09] = alphabet[(int) (msb >>> 16) & 0x1F];
    chars[0x0a] = alphabet[(int) (msb >>> 11) & 0x1F];
    chars[0x0b] = alphabet[(int) (msb >>> 6) & 0x1F];
    chars[0x0c] = alphabet[(int) (msb >>> 1) & 0x1F];

    // encode the overlap between MSBs (1 bit) and LSBs (4 bits)
    long overlap = ((msb & 0x1) << 4) | (lsb >>> 60);
    chars[0x0d] = alphabet[(int) overlap];

    // encode the rest of LSBs
    chars[0x0e] = alphabet[(int) (lsb >>> 55) & 0x1F];
    chars[0x0f] = alphabet[(int) (lsb >>> 50) & 0x1F];
    chars[0x10] = alphabet[(int) (lsb >>> 45) & 0x1F];
    chars[0x11] = alphabet[(int) (lsb >>> 40) & 0x1F];
    chars[0x12] = alphabet[(int) (lsb >>> 35) & 0x1F];
    chars[0x13] = alphabet[(int) (lsb >>> 30) & 0x1F];
    chars[0x14] = alphabet[(int) (lsb >>> 25) & 0x1F];
    chars[0x15] = alphabet[(int) (lsb >>> 20) & 0x1F];
    chars[0x16] = alphabet[(int) (lsb >>> 15) & 0x1F];
    chars[0x17] = alphabet[(int) (lsb >>> 10) & 0x1F];
    chars[0x18] = alphabet[(int) (lsb >>> 5) & 0x1F];
    chars[0x19] = alphabet[(int) lsb & 0x1F];

    return new String(chars);
  }
}
