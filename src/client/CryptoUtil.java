package client;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.security.spec.*;
import java.util.Base64;

public class CryptoUtil {
    private static final String ALGORITHM = "AES";

    public static PrivateKey loadPrivateKey(String path) throws Exception {
        byte[] keyBytes = Files.readAllBytes(Paths.get(path));
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        return KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    public static PublicKey loadPublicKey(String path) throws Exception {
        byte[] keyBytes = Files.readAllBytes(Paths.get(path));
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        return KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    public static SecretKey decryptAESKey(String encryptedBase64) throws Exception {
        byte[] encryptedKey = Base64.getDecoder().decode(encryptedBase64);
        PrivateKey privateKey = loadPrivateKey("client/private_key.der");

        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);

        byte[] aesBytes = cipher.doFinal(encryptedKey);
        return new SecretKeySpec(aesBytes, ALGORITHM);
    }

    public static String encryptMessage(String message, SecretKey aesKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, aesKey);
        byte[] encrypted = cipher.doFinal(message.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    public static void generateAndSaveRSAKeyPair(String publicKeyPath, String privateKeyPath) throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();

        // Save public key
        X509EncodedKeySpec pubSpec = new X509EncodedKeySpec(keyPair.getPublic().getEncoded());
        Files.write(Paths.get(publicKeyPath), pubSpec.getEncoded());

        // Save private key
        PKCS8EncodedKeySpec privSpec = new PKCS8EncodedKeySpec(keyPair.getPrivate().getEncoded());
        Files.write(Paths.get(privateKeyPath), privSpec.getEncoded());
    }
}
