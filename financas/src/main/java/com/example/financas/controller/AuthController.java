package com.example.financas.controller;

import com.example.financas.config.TokenService;
import com.example.financas.entity.Usuario;
import com.example.financas.repository.UsuarioRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Autenticação")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final UsuarioRepository usuarioRepository;
  private final PasswordEncoder passwordEncoder;
  private final TokenService tokenService;

  public AuthController(
      UsuarioRepository usuarioRepository,
      PasswordEncoder passwordEncoder,
      TokenService tokenService) {
    this.usuarioRepository = usuarioRepository;
    this.passwordEncoder = passwordEncoder;
    this.tokenService = tokenService;
  }

  public record LoginRequest(String email, String senha) {}
  public record LoginResponse(String token) {}

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
    Usuario usuario = usuarioRepository.findByEmail(request.email())
        .orElse(null);

    if (usuario == null || !passwordEncoder.matches(request.senha(), usuario.getSenha())) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    String token = tokenService.gerarToken(usuario);
    return ResponseEntity.ok(new LoginResponse(token));
  }
}
