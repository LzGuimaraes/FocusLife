package dev.LzGuimaraes.FocusLifeHub.config;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

import dev.LzGuimaraes.FocusLifeHub.User.UserModel;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.time.Instant;
import java.util.Optional;


@Component
public class TokenConfig {
    @Value("${jwt.secret}")
    private String secret;

    public String generateToken(UserModel user) {

        Algorithm algorithm = Algorithm.HMAC256(secret);
        
        return JWT.create()
            .withClaim("userId", user.getId())
            .withSubject(user.getEmail())
            .withExpiresAt(Instant.now().plusSeconds(860000))
            .withIssuedAt(Instant.now())
            .sign(algorithm);

    }
    public Optional<JWTUserData> validateToken(String token) {
        
        try{
            Algorithm algorithm = Algorithm.HMAC256(secret);

            DecodedJWT decode = JWT.require(algorithm)
                        .build().verify(token);

            return Optional.of(JWTUserData.builder()
                    .userId(decode.getClaim("userId").asLong())
                    .email(decode.getSubject())
                    .build());

        }catch(JWTVerificationException ex){
            return Optional.empty();

        } 
    }
}