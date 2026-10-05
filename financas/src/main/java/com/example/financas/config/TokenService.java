package com.example.financas.config;

import com.example.financas.entity.Usuario;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class TokenService {

  @Value("${jwt.secret:minha-chave-secreta-muito-segura-e-longa-para-jwt}")
  private String secret;

  @Value("${jwt.expiration:86400000}")
  private Long expiration;

  private SecretKey getSigningKey() {
    return Keys.hmacShaKeyFor(secret.getBytes());
  }

  public String gerarToken(Usuario usuario) {
    return Jwts.builder()
        .subject(usuario.getIdUsuario().toString())
        .claim("email", usuario.getEmail())
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + expiration))
        .signWith(getSigningKey())
        .compact();
  }

  public String getSubject(String token) {
    return Jwts.parser()
        .verifyWith(getSigningKey())
        .build()
        .parseSignedClaims(token)
        .getPayload()
        .getSubject();
  }
}
