package com.marius.ptr.vitale.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

@Component
public class SignatureValidationFilter extends OncePerRequestFilter {

    @Autowired
    private LocalTruststoreKeyProvider keyProvider; //

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String signature = request.getHeader("x-signature");
        String bodyHash  = request.getHeader("x-signature-body-hash");

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        chain.doFilter(wrappedRequest, response);

        byte[] rawBody = wrappedRequest.getContentAsByteArray();

        try {
            if (!validateBodyHash(rawBody, bodyHash)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Body hash invalid");
                return;
            }

            RSAPublicKey publicKey = keyProvider.getPublicKey(); // <-- schimbat
            if (!validateSignature(publicKey, rawBody, signature)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Signature invalid");
                return;
            }

        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Validation error");
        }
    }

    private boolean validateBodyHash(byte[] rawBody, String received) throws Exception {
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(rawBody);
        return Base64.getEncoder().encodeToString(hash).equals(received);
    }

    private boolean validateSignature(RSAPublicKey pubKey, byte[] data, String sigBase64) throws Exception {
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(pubKey);
        sig.update(data);
        return sig.verify(Base64.getUrlDecoder().decode(sigBase64));
    }
}
