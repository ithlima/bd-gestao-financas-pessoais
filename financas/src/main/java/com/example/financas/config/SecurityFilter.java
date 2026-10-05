package com.example.financas.config;

import com.example.financas.entity.Usuario;
import com.example.financas.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class SecurityFilter extends OncePerRequestFilter {

  private final TokenService tokenService;
  private final UsuarioRepository usuarioRepository;

  public SecurityFilter(TokenService tokenService, UsuarioRepository usuarioRepository) {
    this.tokenService = tokenService;
    this.usuarioRepository = usuarioRepository;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String token = recuperarToken(request);

    if (token != null) {
      try {
        String subject = tokenService.getSubject(token);
        Usuario usuario = usuarioRepository.findById(Long.valueOf(subject)).orElse(null);

        if (usuario != null && usuario.getAtivo()) {
          var authentication = new UsernamePasswordAuthenticationToken(usuario, null, null);
          SecurityContextHolder.getContext().setAuthentication(authentication);
        }
      } catch (Exception e) {
        // Token inválido ou expirado
      }
    }

    filterChain.doFilter(request, response);
  }

  private String recuperarToken(HttpServletRequest request) {
    String authHeader = request.getHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      return null;
    }
    return authHeader.substring(7);
  }
}
