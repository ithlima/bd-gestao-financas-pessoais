package com.example.financas.repository.projection;

import java.math.BigDecimal;

public interface DreRealizadoProjection {

  Long getCategoriaId();

  String getCategoriaNome();

  String getTipo();

  BigDecimal getTotalRealizado();
}
