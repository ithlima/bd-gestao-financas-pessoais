package com.example.financas.controller;

import com.example.financas.config.TokenService;
import com.example.financas.dto.response.ErroResponse;
import com.example.financas.entity.Usuario;
import com.example.financas.repository.UsuarioRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

  public record LoginRequest(
      @NotBlank(message = "O e-mail é obrigatório")
          @Email(message = "O e-mail deve ser válido")
          String email,
      @NotBlank(message = "A senha é obrigatória") String senha) {}

  public record LoginResponse(String token) {}

  @Operation(
      summary = "Realiza login do usuário",
      description = "Autentica usuário por email e senha e retorna o token JWT.")
  @PostMapping("/login")
  public ResponseEntity<?> login(
      @Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
    Usuario usuario = usuarioRepository.findByEmail(request.email()).orElse(null);

    if (usuario == null || !passwordEncoder.matches(request.senha(), usuario.getSenha())) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(
              ErroResponse.of(
                  401,
                  "Não autorizado",
                  "E-mail ou senha inválidos",
                  httpRequest.getRequestURI(),
                  List.of()));
    }

    if (Boolean.FALSE.equals(usuario.getAtivo())) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(
              ErroResponse.of(
                  403,
                  "Conta inativa",
                  "Esta conta de usuário está desativada",
                  httpRequest.getRequestURI(),
                  List.of()));
    }

    String token = tokenService.gerarToken(usuario);
    return ResponseEntity.ok(new LoginResponse(token));
  }
}
