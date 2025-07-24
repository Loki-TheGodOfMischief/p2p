package common;

import java.io.FileOutputStream;
import java.io.IOException;
import java.security.*;

public class KeyGeneratorUtil {

    public static void generateAndSaveKeyPair(String publicKeyPath, String privateKeyPath) throws NoSuchAlgorithmException, IOException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048); // RSA key size
        KeyPair keyPair = keyGen.generateKeyPair();

        // Save public key
        try (FileOutputStream fos = new FileOutputStream(publicKeyPath)) {
            fos.write(keyPair.getPublic().getEncoded());
        }

        // Save private key
        try (FileOutputStream fos = new FileOutputStream(privateKeyPath)) {
            fos.write(keyPair.getPrivate().getEncoded());
        }

        System.out.println("Keys generated and saved.");
    }

    public static void main(String[] args) {
        try {
            generateAndSaveKeyPair("keys/server_public.key", "keys/server_private.key");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
