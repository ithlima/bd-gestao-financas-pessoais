package com.example.financas.config;

import com.example.financas.entity.Usuario;
import com.example.financas.exception.RegraNegocioException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

  private SecurityUtils() {}

  public static Long validarUsuario(Long usuarioIdParam, Usuario usuarioAutenticado) {
    if (usuarioAutenticado == null) {
      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      if (auth != null && auth.isAuthenticated()) {
        throw new RegraNegocioException("Acesso negado: principal de autenticação inválido");
      }
      return usuarioIdParam;
    }
    if (usuarioIdParam != null && !usuarioIdParam.equals(usuarioAutenticado.getIdUsuario())) {
      throw new RegraNegocioException(
          "Acesso negado: você não tem permissão para acessar ou manipular dados de outro usuário");
    }
    return usuarioAutenticado.getIdUsuario();
  }

  public static Long exigirAutenticado(Usuario usuarioAutenticado) {
    if (usuarioAutenticado == null) {
      throw new RegraNegocioException("Usuário não autenticado");
    }
    return usuarioAutenticado.getIdUsuario();
  }
}
