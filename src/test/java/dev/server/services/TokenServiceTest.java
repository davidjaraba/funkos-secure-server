package dev.server.services;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import dev.common.models.User;
import dev.server.Server;
import dev.server.services.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TokenServiceTest {
    TokenService tokenService;
    final User user = new User(1, "pepe","pepe1234", User.Role.USER);
    final String SECRET = "hola";
    @BeforeEach
    void setUp() {
        tokenService = TokenService.getInstance();
    }

    @Test
    void createToken() {
        String token = tokenService.createToken(user, SECRET, 100);
        assertFalse(token.isBlank());
    }

    @Test
    void verifyToken() {

        String token = tokenService.createToken(user, SECRET, 100);
        assertNotNull(tokenService.verifyToken(token, SECRET));

    }

    @Test
    void verifyInvalidToken() throws InterruptedException{
        String token = tokenService.createToken(user, SECRET, 1);
        Thread.sleep(1000);
        DecodedJWT decodedJWT = tokenService.verifyToken(token, SECRET);
        assertNull(decodedJWT);
    }

    @Test
    void getClaims() {
        String token = tokenService.createToken(user, SECRET, 100);
        Map<String, Claim> claims = tokenService.getClaims(token, SECRET);
        assertEquals(6, claims.size());

    }

    @Test
    void getClaimsInvalidToken() throws InterruptedException{
        String token = tokenService.createToken(user, SECRET, 1);
        Thread.sleep(1000);
        Map<String, Claim> claims = tokenService.getClaims(token, SECRET);
        assertNull(claims);
    }
}