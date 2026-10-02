package com.example.financas.mapper;

import com.example.financas.dto.response.UsuarioResponse;
import com.example.financas.entity.Usuario;

public final class UsuarioMapper {

  private UsuarioMapper() {

  }

  public static UsuarioResponse toResponse(Usuario usuario) {
    return new UsuarioResponse(
        usuario.getIdUsuario(),
        usuario.getNome(),
        usuario.getEmail(),
        usuario.getDataCadastro(),
        usuario.getAtivo());
  }
}
