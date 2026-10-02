package com.example.financas.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Dados de entrada para cadastrar ou atualizar um usuário.
 *
 * <p>É um {@code record} do Java 21: imutável e com os acessores gerados
 * automaticamente. DTOs de requisição são um bom lugar para records, porque o
 * conteúdo nunca deve mudar depois de lido pelo controller.
 *
 * <p><b>Por que existe um DTO em vez de receber a entidade.</b> Se o controller
 * recebesse {@code Usuario} diretamente, o cliente poderia enviar
 * {@code idUsuario} e {@code dataCadastro} e sobrescrever dados que não lhe
 * pertencem decidir. O DTO define exatamente o que o cliente pode informar.
 */
public record UsuarioRequest(
    @NotBlank(message = "O nome é obrigatório")
        @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
        String nome,
    @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "O e-mail deve ser válido")
        @Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres")
        String email,
    @NotBlank(message = "A senha é obrigatória")
        @Size(min = 6, max = 255, message = "A senha deve ter entre 6 e 255 caracteres")
        String senha,
    Boolean ativo) {

  /** Campo opcional: quando não informado, o usuário nasce ativo. */
  public boolean ativoOuPadrao() {
    return ativo == null || ativo;
  }
}
