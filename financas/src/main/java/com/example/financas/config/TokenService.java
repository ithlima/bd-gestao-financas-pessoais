package com.example.financas.config;

import com.example.financas.entity.Usuario;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class TokenService {

  private static final Logger log = LoggerFactory.getLogger(TokenService.class);
  private static final String DEFAULT_DEV_SECRET = "minha-chave-secreta-muito-segura-e-longa-para-jwt";

  @Value("${jwt.secret:" + DEFAULT_DEV_SECRET + "}")
  private String secret;

  @Value("${jwt.expiration:86400000}")
  private Long expiration;

  @PostConstruct
  void validarConfiguracao() {
    if (DEFAULT_DEV_SECRET.equals(secret)) {
      log.warn("JWT usando chave padrão de desenvolvimento. Configure a propriedade/variável 'jwt.secret' em produção.");
    }
    if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
      throw new IllegalStateException("jwt.secret deve ter pelo menos 32 bytes para assinatura HS256/HS384");
    }
  }

  private SecretKey getSigningKey() {
    return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
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
    String subject = Jwts.parser()
        .verifyWith(getSigningKey())
        .build()
        .parseSignedClaims(token)
        .getPayload()
        .getSubject();
    if (subject == null || subject.isBlank()) {
      throw new IllegalArgumentException("Token JWT sem subject válido");
    }
    return subject;
  }
}
