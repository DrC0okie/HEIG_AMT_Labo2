package ch.heigvd.amt.jpa.service;

import io.quarkus.security.jpa.PasswordProvider;
import jakarta.xml.bind.DatatypeConverter;
import org.wildfly.security.password.Password;
import org.wildfly.security.password.interfaces.SimpleDigestPassword;

public class CustomPasswordProvider implements PasswordProvider {
    @Override
    public Password getPassword(String passwordInDatabase) {
        byte[] digest = DatatypeConverter.parseHexBinary(passwordInDatabase);
        return SimpleDigestPassword.createRaw(SimpleDigestPassword.ALGORITHM_SIMPLE_DIGEST_SHA_1, digest);
    }
}

