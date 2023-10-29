package dev.server.services;


import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import dev.common.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

public class TokenService {

    private static TokenService instance;
    private final Logger logger = LoggerFactory.getLogger(TokenService.class);

    private TokenService() {
    }

    public static TokenService getInstance() {
        if (instance == null) {
            instance = new TokenService();
        }
        return instance;
    }

    public String createToken(User user, String secret, long tokenExpirationTime) {
        logger.info("Creando token");
        Algorithm algorithm = Algorithm.HMAC256(secret);
        return JWT.create()
                .withClaim("id", user.id())
                .withClaim("username", user.username())
                .withClaim("password", user.password())
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + tokenExpirationTime))
                .withJWTId(UUID.randomUUID().toString())
                .sign(algorithm);
    }



    public DecodedJWT verifyToken(String token, String secret){

        logger.info("Verificando token...");
        Algorithm algorithm = Algorithm.HMAC256(secret);

        try{
            DecodedJWT verifiedToken = JWT.require(algorithm)
                    .build()
                    .verify(token);


            logger.info("Token verificado");

            return verifiedToken;

        }catch (Exception e){
            logger.error("Error al verificar token "+e);
            return null;
        }

    }

    public Map<String, Claim> getClaims(String token, String secret){

        logger.debug("Obteniendo claims del token");

        Algorithm algorithm = Algorithm.HMAC256(secret);

        try{
            DecodedJWT decodedJWT = JWT.require(algorithm)
                    .build()
                    .verify(token);


            logger.info("Token verificado");

            return decodedJWT.getClaims();

        }catch (Exception e){
            logger.error("Error al verificar token "+e);
            return null;
        }

    }


}
