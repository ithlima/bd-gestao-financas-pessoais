package com.example.financas.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * Representação pública de um usuário.
 *
 * <p><b>Repare no que não existe aqui: a senha.</b> Este é o motivo prático de
 * existir um DTO de resposta. Se o controller devolvesse a entidade
 * {@code Usuario} diretamente, o hash da senha seria serializado no JSON e
 * enviado ao cliente. Um DTO permite escolher exatamente o que sai.
 */
public record UsuarioResponse(
    Long idUsuario,
    String nome,
    String email,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime dataCadastro,
    Boolean ativo) {}
