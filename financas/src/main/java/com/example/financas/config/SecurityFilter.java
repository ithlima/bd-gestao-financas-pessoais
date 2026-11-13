package com.example.financas.config;

import com.example.financas.entity.Usuario;
import com.example.financas.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class SecurityFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(SecurityFilter.class);

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

        if (usuario != null && Boolean.TRUE.equals(usuario.getAtivo())) {
          var authentication = new UsernamePasswordAuthenticationToken(usuario, null, List.of());
          SecurityContextHolder.getContext().setAuthentication(authentication);
        }
      } catch (Exception e) {
        SecurityContextHolder.clearContext();
        log.debug("Falha ao validar token JWT em {}: {}", request.getRequestURI(), e.getMessage());
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
