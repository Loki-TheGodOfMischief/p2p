package common;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.*;

public class KeyExchangeProtocol {
    private SecretKey aesKey;

    public void performServerHandshake(ObjectInputStream in, ObjectOutputStream out) throws Exception {
        // Generate RSA key pair
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();

        // Send public key to client
        out.writeObject(kp.getPublic());

        // Receive AES key encrypted with RSA public key
        byte[] encryptedAESKey = (byte[]) in.readObject();
        Cipher rsaCipher = Cipher.getInstance("RSA");
        rsaCipher.init(Cipher.DECRYPT_MODE, kp.getPrivate());
        byte[] aesKeyBytes = rsaCipher.doFinal(encryptedAESKey);

        aesKey = new SecretKeySpec(aesKeyBytes, "AES");
    }

    public SecretKey getAESKey() {
        return aesKey;
    }
}
