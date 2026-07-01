package davi.spf.supportflow.auth.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(
        RSAPrivateKey privateKey,
        RSAPublicKey publicKey,
        Long expirationMinutes
) {
}
