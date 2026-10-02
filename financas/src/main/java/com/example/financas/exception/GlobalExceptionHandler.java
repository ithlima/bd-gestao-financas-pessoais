package com.example.financas.exception;

import com.example.financas.dto.response.ErroResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  private static final Logger registro = LoggerFactory.getLogger("com.example.financas.api");

  private static final Pattern NOME_DA_CONSTRAINT = Pattern.compile("constraint \\[([^\\]]+)]");

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErroResponse> tratarCorpoInvalido(
      MethodArgumentNotValidException ex, HttpServletRequest request) {

    List<String> detalhes =
        ex.getBindingResult().getFieldErrors().stream()
            .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
            .toList();

    return erro(
        HttpStatus.BAD_REQUEST,
        "Erro de validação",
        "Um ou mais campos estão inválidos",
        request,
        detalhes);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErroResponse> tratarViolacaoDeConstraint(
      ConstraintViolationException ex, HttpServletRequest request) {

    List<String> detalhes =
        ex.getConstraintViolations().stream()
            .map(this::formatarViolacao)
            .toList();

    return erro(
        HttpStatus.BAD_REQUEST,
        "Erro de validação",
        "Um ou mais parâmetros estão inválidos",
        request,
        detalhes);
  }

  private String formatarViolacao(ConstraintViolation<?> violacao) {
    return violacao.getPropertyPath() + ": " + violacao.getMessage();
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErroResponse> tratarCorpoIlegivel(
      HttpMessageNotReadableException ex, HttpServletRequest request) {

    registro.debug(
        "Corpo ilegível em {} {}: {}",
        request.getMethod(),
        request.getRequestURI(),
        ex.getMostSpecificCause().getMessage());

    String mensagem = traduzirErroDeLeitura(ex.getMostSpecificCause().getMessage());

    return erro(HttpStatus.BAD_REQUEST, "Requisição inválida", mensagem, request, List.of());
  }

  private String traduzirErroDeLeitura(String causa) {
    if (causa == null) {
      return "O corpo da requisição não pôde ser lido";
    }

    if (causa.contains("Required request body is missing")) {
      return "O corpo da requisição é obrigatório e não foi enviado";
    }

    if (causa.contains("not one of the values accepted for Enum class")) {
      String aceitos = extrairEntre(causa, "[", "]");
      return "Valor inválido para um campo de opções. Valores aceitos: " + aceitos;
    }

    if (causa.contains("LocalDate")
        || causa.contains("LocalDateTime")
        || causa.contains("DateTimeParseException")
        || causa.contains("could not be parsed")
        || causa.contains("Failed to deserialize java.time")) {
      return "Data inválida. Use o formato ISO: AAAA-MM-DD (exemplo: 2026-03-10)";
    }

    if (causa.contains("Unexpected end-of-input")
        || causa.contains("Unexpected character")
        || causa.contains("JSON parse error")) {
      return "O JSON enviado está malformado. Verifique chaves, aspas e vírgulas";
    }

    return "O corpo da requisição não pôde ser lido";
  }

  private String extrairEntre(String texto, String abre, String fecha) {
    int i = texto.indexOf(abre);
    int f = texto.indexOf(fecha, i + 1);
    return (i >= 0 && f > i) ? texto.substring(i + 1, f) : "desconhecidos";
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErroResponse> tratarTipoDeParametroInvalido(
      MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

    String tipoEsperado =
        ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "desconhecido";
    String mensagem = "O parâmetro '" + ex.getName() + "' recebeu um valor inválido";

    if (ex.getRequiredType() != null && ex.getRequiredType().isEnum()) {
      List<String> valores =
          java.util.Arrays.stream(ex.getRequiredType().getEnumConstants())
              .map(Object::toString)
              .toList();
      mensagem += ". Valores aceitos: " + String.join(", ", valores);
    } else if ("LocalDate".equals(tipoEsperado)) {
      mensagem += ". Use o formato ISO: AAAA-MM-DD (exemplo: 2026-03-10)";
    } else if ("Long".equals(tipoEsperado) || "Integer".equals(tipoEsperado)) {
      mensagem += ". Era esperado um número inteiro";
    }

    return erro(HttpStatus.BAD_REQUEST, "Parâmetro inválido", mensagem, request,
        List.of("parâmetro: " + ex.getName() + ", valor: " + ex.getValue()
            + ", esperado: " + tipoEsperado));
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErroResponse> tratarParametroAusente(
      MissingServletRequestParameterException ex, HttpServletRequest request) {

    String mensagem =
        "O parâmetro obrigatório '" + ex.getParameterName() + "' não foi informado";

    return erro(HttpStatus.BAD_REQUEST, "Parâmetro ausente", mensagem, request,
        List.of("parâmetro: " + ex.getParameterName() + ", tipo: "
            + ex.getParameterType()));
  }

  @ExceptionHandler(RecursoNaoEncontradoException.class)
  public ResponseEntity<ErroResponse> tratarNaoEncontrado(
      RecursoNaoEncontradoException ex, HttpServletRequest request) {
    return erro(HttpStatus.NOT_FOUND, "Recurso não encontrado", ex.getMessage(), request, List.of());
  }

  @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
  public ResponseEntity<ErroResponse> tratarRotaInexistente(
      Exception ex, HttpServletRequest request) {

    return erro(
        HttpStatus.NOT_FOUND,
        "Rota não encontrada",
        "Não existe endpoint para " + request.getMethod() + " " + request.getRequestURI(),
        request,
        List.of("Consulte a documentação da API para ver as rotas disponíveis"));
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ErroResponse> tratarMetodoNaoSuportado(
      HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {

    List<String> suportados =
        ex.getSupportedHttpMethods() == null
            ? List.of()
            : ex.getSupportedHttpMethods().stream().map(Object::toString).sorted().toList();

    String mensagem = "O método " + ex.getMethod() + " não é aceito nesta rota";
    if (!suportados.isEmpty()) {
      mensagem += ". Métodos aceitos: " + String.join(", ", suportados);
    }

    return erro(HttpStatus.METHOD_NOT_ALLOWED, "Método não permitido", mensagem, request,
        List.of("rota: " + request.getRequestURI()));
  }

  @ExceptionHandler(RecursoDuplicadoException.class)
  public ResponseEntity<ErroResponse> tratarDuplicado(
      RecursoDuplicadoException ex, HttpServletRequest request) {
    return erro(HttpStatus.CONFLICT, "Registro duplicado", ex.getMessage(), request, List.of());
  }

  @ExceptionHandler(RecursoEmUsoException.class)
  public ResponseEntity<ErroResponse> tratarRecursoEmUso(
      RecursoEmUsoException ex, HttpServletRequest request) {
    return erro(HttpStatus.CONFLICT, "Recurso em uso", ex.getMessage(), request, List.of());
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErroResponse> tratarIntegridade(
      DataIntegrityViolationException ex, HttpServletRequest request) {

    String nomeDaConstraint = extrairNomeDaConstraint(ex);
    String mensagem = traduzirConstraint(nomeDaConstraint);

    return erro(HttpStatus.CONFLICT, "Conflito de integridade", mensagem, request,
        List.of("constraint: " + nomeDaConstraint));
  }

  private String extrairNomeDaConstraint(DataIntegrityViolationException ex) {
    String texto = ex.getMostSpecificCause().getMessage();
    if (texto == null) {
      return "desconhecida";
    }
    Matcher matcher = NOME_DA_CONSTRAINT.matcher(texto);
    if (matcher.find()) {
      return matcher.group(1);
    }

    return texto.contains("Duplicate entry") ? "restricao de unicidade" : "desconhecida";
  }

  private String traduzirConstraint(String nome) {
    if (nome == null) {
      return "A operação viola uma regra de integridade do banco de dados";
    }
    return switch (nome) {
      case "fk_titulo_usuario", "fk_categoria_usuario", "fk_conta_usuario",
              "fk_movimentacao_usuario" ->
          "Não é possível excluir este usuário porque existem títulos, categorias, contas ou "
              + "movimentações vinculados a ele. Exclua ou transfira esses registros primeiro";
      case "fk_titulo_categoria", "fk_movimentacao_categoria" ->
          "Não é possível excluir esta categoria porque existem títulos ou movimentações que a "
              + "utilizam. Reclassifique esses registros primeiro";
      case "fk_movimentacao_conta" ->
          "Não é possível excluir esta conta porque existem movimentações realizadas nela. "
              + "Exclua as movimentações ou use outra conta";
      case "fk_movimentacao_titulo" ->
          "Não é possível excluir este título porque existem movimentações vinculadas a ele. "
              + "Estorne as movimentações primeiro";
      case "uq_categoria_nome_usuario" ->
          "Já existe uma categoria com este nome para este usuário";
      case "uq_conta_nome_usuario" ->
          "Já existe uma conta com este nome para este usuário";
      default ->
          "A operação viola uma regra de integridade do banco de dados (constraint: " + nome + ")";
    };
  }

  @ExceptionHandler(RegraNegocioException.class)
  public ResponseEntity<ErroResponse> tratarRegraNegocio(
      RegraNegocioException ex, HttpServletRequest request) {
    return erro(
        HttpStatus.UNPROCESSABLE_ENTITY,
        "Regra de negócio violada",
        ex.getMessage(),
        request,
        List.of());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErroResponse> tratarErroInesperado(
      Exception ex, HttpServletRequest request) {

    log.error(
        "Erro inesperado em {} {}: {}",
        request.getMethod(),
        request.getRequestURI(),
        ex.getMessage(),
        ex);

    return erro(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Erro interno",
        "Ocorreu um erro inesperado ao processar a requisição. "
            + "A causa foi registrada no log do servidor",
        request,
        List.of());
  }

  private ResponseEntity<ErroResponse> erro(
      HttpStatusCode status,
      String tipo,
      String mensagem,
      HttpServletRequest request,
      List<String> detalhes) {

    return ResponseEntity.status(status)
        .body(ErroResponse.of(status.value(), tipo, mensagem, request.getRequestURI(), detalhes));
  }
}
