package com.marius.ptr.app;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.beans.factory.annotation.Value;
import java.net.URL;
import java.security.interfaces.RSAPublicKey;

public class JwksKeyProvider implements PublicKeyProvider {

    @Value("${jwks.endpoint.url}")
    private String jwksUrl;

    @Override
    public RSAPublicKey getPublicKey() throws Exception {
        JWKSet jwkSet = JWKSet.load(new URL(jwksUrl)); // Nimbus fetch + parsare automată
        RSAKey rsaKey = (RSAKey) jwkSet.getKeys().get(0); // sau filtrezi după kid
        return rsaKey.toRSAPublicKey(); // Nimbus îți dă direct PublicKey
    }
}
