package com.pyplatform;
import java.security.*; import java.util.Base64; import javax.crypto.SecretKeyFactory; import javax.crypto.spec.PBEKeySpec;
final class Security {
  static String hash(String pw) { byte[] s = new byte[16]; new SecureRandom().nextBytes(s); return Base64.getEncoder().encodeToString(s) + ":" + pbk(pw, s); }
  static boolean check(String pw, String stored) {
    String[] p = stored.split(":");
    return MessageDigest.isEqual(pbk(pw, Base64.getDecoder().decode(p[0])).getBytes(), p[1].getBytes());
  }
  private static String pbk(String pw, byte[] salt) {
    try { return Base64.getEncoder().encodeToString(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(new PBEKeySpec(pw.toCharArray(), salt, 65000, 256)).getEncoded()); }
    catch (Exception e) { throw new IllegalStateException(e); }
  }
}
