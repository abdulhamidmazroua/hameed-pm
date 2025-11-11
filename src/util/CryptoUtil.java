package util;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.*;
import java.util.Base64;

public class CryptoUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    public static byte[] generateRandomBytes(int bytes) {
        byte[] randomBytes = new byte[bytes];
        RANDOM.nextBytes(randomBytes);
        return randomBytes;
    }

    public static byte[] deriveKey(char[] password, byte[] salt, int iterations) throws Exception{
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, 256);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return skf.generateSecret(spec).getEncoded();
    }

    public static SecretKey keyFromBytes(byte[] keyBytes) {
        return new SecretKeySpec(keyBytes, "AES");
    }

    public static byte[] encrypt(SecretKey key, byte[] plaintext, byte[] iv) throws Exception {
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
        return c.doFinal(plaintext);
    }

    public static byte[] decrypt(SecretKey key, byte[] ciphertext, byte[] iv)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
        return c.doFinal(ciphertext);
    }

    public static byte[] deriveHash(char[] plaintext, byte[] salt, int iterations) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(plaintext, salt, iterations, 256);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        try {
            return skf.generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }

    public static String computeHmac(byte[] signingKey, byte[] salt, byte[] iv, int iterations, byte[] ciphertext) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(signingKey, "HmacSHA256"));
        mac.update(salt);
        mac.update(iv);
        mac.update(ByteBuffer.allocate(4).putInt(iterations).array());
        mac.update(ciphertext);
        return Base64.getEncoder().encodeToString(mac.doFinal());

    }

}
