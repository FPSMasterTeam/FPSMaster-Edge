package top.fpsmaster.utils.crypto;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Random;

public class CryptUtils {
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";

    public static String encrypt(String text, String key) {
        try {
            Key secretKey = generateKey(key);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String decrypt(String encryptedText, String key) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            throw new IllegalArgumentException("Encrypted text cannot be null or empty");
        }
        try {
            Key secretKey = generateKey(key);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedText);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Key generateKey(String key) {
        String md5Hash = getMD5Hash(key);
        if (md5Hash == null) {
            throw new IllegalArgumentException("Failed to generate MD5 hash");
        }
        byte[] keyBytes = md5Hash.getBytes(StandardCharsets.UTF_8);
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }

    private static String getMD5Hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                hexString.append(String.format("%02x", b & 0xFF));
            }
            return hexString.toString().toLowerCase();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getSHA256Hash(String input) {
        try {
            MessageDigest sha256Digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = sha256Digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }


    public static String aesEncrypt(String text, String key) {
        try {
            IvParameterSpec iv = new IvParameterSpec("0102030405060708".getBytes(StandardCharsets.UTF_8));
            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);

            byte[] encrypted = cipher.doFinal(text.getBytes());

            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception ex) {
            return "";
        }
    }

    public static String rsaEncrypt(String text, String pubKey, String modulus) {
        text = new StringBuilder(text).reverse().toString();
        BigInteger rs = new BigInteger(String.format("%x", new BigInteger(1, text.getBytes())), 16)
                .modPow(new BigInteger(pubKey, 16), new BigInteger(modulus, 16));
        StringBuilder r = new StringBuilder(rs.toString(16));
        if (r.length() >= 256) {
            return r.substring(r.length() - 256);
        } else {
            while (r.length() < 256) {
                r.insert(0, 0);
            }
            return r.toString();
        }
    }


    public static String createSecretKey(int length) {
        String shits = "0123456789abcdefghijklmnopqrstuvwxyz";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; ++i) {
            sb.append(shits.charAt(new Random().nextInt(shits.length())));
        }
        return sb.toString();
    }
}



