package com.marius.ptr.app;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class CommonApiAutoConfiguration {

    @Bean
    @ConditionalOnProperty(name = "jwks.endpoint.url")
    @ConditionalOnMissingBean(PublicKeyProvider.class)
    public JwksKeyProvider jwksKeyProvider() {
        return new JwksKeyProvider();
    }

    @Bean
    @ConditionalOnProperty(name = "app.truststore.path")
    @ConditionalOnMissingBean(PublicKeyProvider.class)
    public LocalTruststoreKeyProvider localTruststoreKeyProvider() {
        return new LocalTruststoreKeyProvider();
    }

    @Bean
    @ConditionalOnWebApplication
    @ConditionalOnBean(PublicKeyProvider.class)
    @ConditionalOnMissingBean(SignatureValidationFilter.class)
    public SignatureValidationFilter signatureValidationFilter(PublicKeyProvider keyProvider) {
        return new SignatureValidationFilter(keyProvider);
    }
}
