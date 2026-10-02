package com.example.financas.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record UsuarioResponse(
    Long idUsuario,
    String nome,
    String email,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime dataCadastro,
    Boolean ativo) {}
