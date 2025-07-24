package client;

import java.io.*;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;

public class AESUtil {
    public static byte[] encryptObject(Serializable object, SecretKey aesKey) throws Exception {
        // Serialize object to byte[]
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bos);
        out.writeObject(object);
        out.flush();
        byte[] serialized = bos.toByteArray();

        // Encrypt bytes
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, aesKey);
        return cipher.doFinal(serialized);
    }

    public static Object decryptObject(byte[] encrypted, SecretKey aesKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, aesKey);
        byte[] decrypted = cipher.doFinal(encrypted);

        // Deserialize object
        ByteArrayInputStream bis = new ByteArrayInputStream(decrypted);
        ObjectInputStream in = new ObjectInputStream(bis);
        return in.readObject();
    }
}

