package client;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;

public class AuthenticationUtil {
    private static final String PRIVATE_KEY_FILE = "client/private_key.der";

    public static byte[] signChallenge(String challenge) throws Exception {
        PrivateKey privateKey = CryptoUtil.loadPrivateKey(PRIVATE_KEY_FILE);
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(challenge.getBytes(StandardCharsets.UTF_8));
        return signature.sign();
    }

    public static String encodeBase64(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }
}
