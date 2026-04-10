package com.marius.ptr.app;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.interfaces.RSAPublicKey;

public class LocalTruststoreKeyProvider implements PublicKeyProvider {

    @Value("${app.truststore.path}")
    private Resource truststorePath;

    @Value("${app.truststore.password}")
    private String truststorePassword;

    @Value("${app.truststore.alias}")
    private String keyAlias;

    private RSAPublicKey cachedKey;

    @PostConstruct
    public void loadKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream is = truststorePath.getInputStream()) {
            keyStore.load(is, truststorePassword.toCharArray());
        }
        Certificate cert = keyStore.getCertificate(keyAlias);
        cachedKey = (RSAPublicKey) cert.getPublicKey();
    }

    @Override
    public RSAPublicKey getPublicKey() {
        return cachedKey;
    }
}
