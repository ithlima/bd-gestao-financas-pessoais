package com.example.financas.config;

import com.example.financas.entity.Usuario;
import com.example.financas.exception.RegraNegocioException;

public class SecurityUtils {

  private SecurityUtils() {}

  public static Long validarUsuario(Long usuarioIdParam, Usuario usuarioAutenticado) {
    if (usuarioAutenticado != null && usuarioIdParam != null && !usuarioIdParam.equals(usuarioAutenticado.getIdUsuario())) {
      throw new RegraNegocioException("Acesso negado: você não tem permissão para acessar ou manipular dados de outro usuário");
    }
    return usuarioAutenticado != null ? usuarioAutenticado.getIdUsuario() : usuarioIdParam;
  }
}
