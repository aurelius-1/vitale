package com.marius.ptr.app;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignatureValidationTest {

    private PrivateKey privateKey;
    private RSAPublicKey publicKey;

    @BeforeEach
    void loadKeys() throws Exception {
        privateKey = loadPrivateKey("src/test/resources/test_key_signature.pem");
        publicKey  = (RSAPublicKey) loadPublicKey("src/test/resources/cheie_pub_sign.pem");
    }

    @Test
    void shouldValidateSignatureAndBodyHash() throws Exception {
        byte[] body = "{\"hello\":\"world\"}".getBytes(StandardCharsets.UTF_8);

        String bodyHash = computeBodyHash(body);
        String signature = sign(body, privateKey);

        assertTrue(validateBodyHash(body, bodyHash));
        assertTrue(validateSignature(publicKey, body, signature));
    }

    @Test
    void shouldRejectTamperedBody() throws Exception {
        byte[] originalBody  = "{\"hello\":\"world\"}".getBytes(StandardCharsets.UTF_8);
        byte[] tamperedBody  = "{\"hello\":\"hacked\"}".getBytes(StandardCharsets.UTF_8);

        String bodyHash = computeBodyHash(originalBody);
        String signature = sign(originalBody, privateKey);

        assertFalse(validateBodyHash(tamperedBody, bodyHash));
        assertFalse(validateSignature(publicKey, tamperedBody, signature));
    }


    private String sign(byte[] data, PrivateKey key) throws Exception {
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(key);
        sig.update(data);
        return Base64.getEncoder().encodeToString(sig.sign());
    }

    private String computeBodyHash(byte[] body) throws Exception {
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(body);
        return Base64.getEncoder().encodeToString(hash);
    }

    private boolean validateBodyHash(byte[] rawBody, String received) throws Exception {
        return computeBodyHash(rawBody).equals(received);
    }

    private boolean validateSignature(RSAPublicKey pubKey, byte[] data, String sigBase64) throws Exception {
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(pubKey);
        sig.update(data);
        return sig.verify(Base64.getDecoder().decode(sigBase64));
    }

    private PrivateKey loadPrivateKey(String path) throws Exception {
        String pem = Files.readString(Path.of(path))
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(pem);
        return KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(decoded));
    }

    private PublicKey loadPublicKey(String path) throws Exception {
        String pem = Files.readString(Path.of(path))
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(pem);
        return KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(decoded));
    }
}