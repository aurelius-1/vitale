package com.marius.ptr.app;

import java.security.interfaces.RSAPublicKey;

public interface PublicKeyProvider {
    RSAPublicKey getPublicKey() throws Exception;
}
