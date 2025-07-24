package server;

import java.io.*;
import java.security.*;
import java.security.spec.*;
import javax.crypto.Cipher;

public class ServerAuth {
    public static boolean authenticate(ObjectInputStream in, ObjectOutputStream out) throws Exception {
        PublicKey clientPublicKey = (PublicKey) in.readObject();

        // Send challenge
        byte[] challenge = new byte[32];
        new SecureRandom().nextBytes(challenge);
        out.writeObject(challenge);

        // Receive signature
        byte[] signature = (byte[]) in.readObject();

        // Verify signature
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(clientPublicKey);
        sig.update(challenge);
        return sig.verify(signature);
    }
}
