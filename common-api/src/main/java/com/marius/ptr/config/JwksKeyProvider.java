package com.marius.ptr.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.security.interfaces.RSAPublicKey;

@Component
public class JwksKeyProvider {

    @Value("${jwks.endpoint.url}")
    private String jwksUrl;

    public RSAPublicKey getPublicKey() throws Exception {
        JWKSet jwkSet = JWKSet.load(new URL(jwksUrl)); // Nimbus fetch + parsare automată
        RSAKey rsaKey = (RSAKey) jwkSet.getKeys().get(0); // sau filtrezi după kid
        return rsaKey.toRSAPublicKey(); // Nimbus îți dă direct PublicKey
    }
}
