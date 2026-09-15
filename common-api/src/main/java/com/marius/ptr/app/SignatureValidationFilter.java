package com.marius.ptr.app;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

public class SignatureValidationFilter extends OncePerRequestFilter {

    private final PublicKeyProvider keyProvider;

    public SignatureValidationFilter(PublicKeyProvider keyProvider) {
        this.keyProvider = keyProvider;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Health/metrics endpoints are polled by infrastructure (load balancers,
        // Kubernetes probes) that cannot attach a request signature.
        return request.getRequestURI().startsWith("/actuator/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String signature = request.getHeader("x-signature");
        String bodyHash  = request.getHeader("x-signature-body-hash");

        // Buffer the body ourselves so we can validate it before anything downstream runs.
        byte[] rawBody = request.getInputStream().readAllBytes();

        try {
            if (!validateBodyHash(rawBody, bodyHash)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Body hash invalid");
                return;
            }

            RSAPublicKey publicKey = keyProvider.getPublicKey();
            if (!validateSignature(publicKey, rawBody, signature)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Signature invalid");
                return;
            }

        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Validation error");
            return;
        }

        // Only reached once the body hash and signature have both been verified.
        // request's own input stream is already drained, so downstream reads must be
        // served from the bytes we buffered above rather than the (now-empty) original stream.
        chain.doFilter(wrapWithBody(request, rawBody), response);
    }

    private boolean validateBodyHash(byte[] rawBody, String received) throws Exception {
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(rawBody);
        return Base64.getEncoder().encodeToString(hash).equals(received);
    }

    private boolean validateSignature(RSAPublicKey pubKey, byte[] data, String sigBase64) throws Exception {
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(pubKey);
        sig.update(data);
        return sig.verify(Base64.getDecoder().decode(sigBase64));
    }

    private static HttpServletRequest wrapWithBody(HttpServletRequest request, byte[] body) {
        return new HttpServletRequestWrapper(request) {
            @Override
            public ServletInputStream getInputStream() {
                ByteArrayInputStream buffer = new ByteArrayInputStream(body);
                return new ServletInputStream() {
                    @Override
                    public boolean isFinished() {
                        return buffer.available() == 0;
                    }

                    @Override
                    public boolean isReady() {
                        return true;
                    }

                    @Override
                    public void setReadListener(ReadListener readListener) {
                    }

                    @Override
                    public int read() {
                        return buffer.read();
                    }
                };
            }

            @Override
            public BufferedReader getReader() throws IOException {
                return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
            }
        };
    }
}
