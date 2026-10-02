package com.example.financas.mapper;

import com.example.financas.dto.response.UsuarioResponse;
import com.example.financas.entity.Usuario;

/**
 * Conversão entre a entidade {@code Usuario} e seus DTOs.
 *
 * <p><b>Por que uma classe só para isso.</b> Sem o mapper, a conversão
 * entidade → DTO se espalharia por todos os services, repetida em cada método.
 * Concentrando-a aqui, existe um único lugar para responder "o que a API expõe
 * de um usuário?" — e é justamente aqui que se garante que a senha nunca sai.
 *
 * <p>Os métodos são {@code static} porque não há estado a guardar: é conversão
 * pura, entrada → saída.
 */
public final class UsuarioMapper {

  private UsuarioMapper() {
    // classe utilitária: não deve ser instanciada
  }

  /** Entidade → DTO de resposta. Note a ausência proposital do campo senha. */
  public static UsuarioResponse toResponse(Usuario usuario) {
    return new UsuarioResponse(
        usuario.getIdUsuario(),
        usuario.getNome(),
        usuario.getEmail(),
        usuario.getDataCadastro(),
        usuario.getAtivo());
  }
}
