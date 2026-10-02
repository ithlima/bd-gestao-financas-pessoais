package com.example.financas.repository.projection;

import java.math.BigDecimal;

public interface DrePrevisaoProjection {

  Long getCategoriaId();

  String getCategoriaNome();

  String getTipo();

  BigDecimal getTotalPrevisto();
}
