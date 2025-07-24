package server;

import javax.crypto.*;
import javax.crypto.spec.*;
import java.io.*;
import java.security.*;

public class AESUtil {
    public static byte[] encrypt(Serializable obj, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(obj);
        oos.close();

        return cipher.doFinal(baos.toByteArray());
    }

    public static Object decrypt(byte[] data, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, key);

        ByteArrayInputStream bais = new ByteArrayInputStream(cipher.doFinal(data));
        ObjectInputStream ois = new ObjectInputStream(bais);
        return ois.readObject();
    }
}