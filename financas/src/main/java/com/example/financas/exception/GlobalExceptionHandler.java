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

/**
 * Centraliza o tratamento de exceções de <b>todos</b> os controllers.
 *
 * <h2>Princípio: nenhum erro sem explicação</h2>
 *
 * <p>O objetivo desta classe é que <b>nenhuma</b> falha chegue ao cliente como um
 * "erro interno" genérico. Cada situação tem um status HTTP correto e uma
 * mensagem que diz o que aconteceu e, quando possível, o que fazer.
 *
 * <p>Um detalhe importante aprendido na prática: um {@code @ExceptionHandler}
 * genérico para {@code Exception} <b>atrapalha</b> quando é largo demais. O Spring
 * já trata corretamente vários erros do cliente (JSON malformado, parâmetro
 * faltando, rota inexistente) e o handler genérico os rebaixa para 500, perdendo
 * a informação. Por isso os casos específicos abaixo existem: eles têm precedência
 * sobre o genérico.
 *
 * <h2>Mapa completo</h2>
 *
 * <table border="1">
 *   <caption>Exceção para status HTTP</caption>
 *   <tr><th>Exceção</th><th>HTTP</th><th>Quando ocorre</th></tr>
 *   <tr><td>{@link MethodArgumentNotValidException}</td><td>400</td>
 *       <td>Bean Validation reprovou um campo do corpo</td></tr>
 *   <tr><td>{@link ConstraintViolationException}</td><td>400</td>
 *       <td>validação em parâmetro de método</td></tr>
 *   <tr><td>{@link HttpMessageNotReadableException}</td><td>400</td>
 *       <td>JSON malformado, enum ou data inválidos no corpo</td></tr>
 *   <tr><td>{@link MethodArgumentTypeMismatchException}</td><td>400</td>
 *       <td>parâmetro de query/rota com tipo inválido</td></tr>
 *   <tr><td>{@link MissingServletRequestParameterException}</td><td>400</td>
 *       <td>parâmetro obrigatório ausente</td></tr>
 *   <tr><td>{@link RecursoNaoEncontradoException}</td><td>404</td>
 *       <td>id inexistente</td></tr>
 *   <tr><td>{@link NoResourceFoundException} / {@link NoHandlerFoundException}</td><td>404</td>
 *       <td>rota inexistente</td></tr>
 *   <tr><td>{@link HttpRequestMethodNotSupportedException}</td><td>405</td>
 *       <td>verbo HTTP não suportado pela rota</td></tr>
 *   <tr><td>{@link RecursoDuplicadoException}</td><td>409</td>
 *       <td>e-mail/nome repetido</td></tr>
 *   <tr><td>{@link RecursoEmUsoException}</td><td>409</td>
 *       <td>exclusão bloqueada por registros dependentes</td></tr>
 *   <tr><td>{@link DataIntegrityViolationException}</td><td>409</td>
 *       <td>violação de FK ou de unicidade detectada pelo banco</td></tr>
 *   <tr><td>{@link RegraNegocioException}</td><td>422</td>
 *       <td>regra de domínio violada</td></tr>
 *   <tr><td>{@code Exception}</td><td>500</td>
 *       <td>erro realmente inesperado (não vaza detalhe interno)</td></tr>
 * </table>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /**
   * Log de detalhe técnico para erros do cliente.
   *
   * <p>Separado do log de erro: um JSON malformado é culpa de quem chamou a API e
   * não deve poluir o log de erros do servidor. Mas o detalhe cru precisa ficar
   * registrado em algum lugar, já que não é enviado ao cliente.
   */
  private static final Logger registro = LoggerFactory.getLogger("com.example.financas.api");

  /**
   * Extrai o nome da constraint citado pelo banco.
   *
   * <p>Exemplo: {@code constraint [fk_titulo_categoria]} → {@code fk_titulo_categoria}
   */
  private static final Pattern NOME_DA_CONSTRAINT = Pattern.compile("constraint \\[([^\\]]+)]");

  // ==================================================================
  // 400 — erros do cliente no corpo da requisição
  // ==================================================================

  /**
   * Bean Validation reprovou um ou mais campos do corpo.
   *
   * <p>Devolve <b>todos</b> os campos reprovados de uma vez, e não apenas o
   * primeiro — assim o cliente corrige tudo em uma única passada.
   */
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

  /** Validação de parâmetro de método (ex.: {@code @Validated} em query params). */
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

  // ==================================================================
  // 400 — erros do cliente no formato dos dados
  // ==================================================================

  /**
   * O corpo da requisição não pôde ser lido.
   *
   * <p>Cobre três casos comuns, e a mensagem é traduzida para cada um:
   *
   * <ul>
   *   <li><b>JSON malformado</b> — chave não fechada, vírgula sobrando;</li>
   *   <li><b>valor de enum inválido</b> — ex.: {@code "tipo": "INVALIDO"};</li>
   *   <li><b>data inválida</b> — ex.: {@code "31/02/2026"} no lugar de
   *       {@code "2026-02-28"}.</li>
   * </ul>
   *
   * <p>A mensagem crua do Jackson é registrada no <b>log do servidor</b>, mas não
   * é enviada ao cliente: além de citar classes Java internas, ela contém
   * deslocamentos de byte e detalhes de configuração da biblioteca
   * ({@code StreamReadFeature}, {@code REDACTED}) que não ajudam quem consome a
   * API — e revelam estrutura interna. Mesma decisão tomada para o erro de banco
   * de dados.
   */
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

  /**
   * Traduz a mensagem crua do Jackson para uma explicação útil.
   *
   * <p>A ordem das verificações importa, e o texto procurado é generoso de
   * propósito: o Jackson 2 e o Jackson 3 descrevem o mesmo erro de formas
   * diferentes. Uma data inválida, por exemplo, aparece como
   * {@code Text '31/02/2026' could not be parsed} no Jackson 2 e como
   * {@code Failed to deserialize java.time.LocalDate} no Jackson 3. Procurar
   * apenas uma das formas deixaria o outro caso cair na mensagem genérica.
   */
  private String traduzirErroDeLeitura(String causa) {
    if (causa == null) {
      return "O corpo da requisição não pôde ser lido";
    }

    // Corpo ausente. A mensagem crua cita a assinatura completa do método do
    // controller; o cliente só precisa saber que faltou enviar o corpo.
    if (causa.contains("Required request body is missing")) {
      return "O corpo da requisição é obrigatório e não foi enviado";
    }

    if (causa.contains("not one of the values accepted for Enum class")) {
      String aceitos = extrairEntre(causa, "[", "]");
      return "Valor inválido para um campo de opções. Valores aceitos: " + aceitos;
    }

    // Data: cobre Jackson 2 (DateTimeParseException, could not be parsed) e
    // Jackson 3 (Failed to deserialize java.time.LocalDate, ValueInstant).
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

  /** Extrai o primeiro trecho de texto entre dois delimitadores. */
  private String extrairEntre(String texto, String abre, String fecha) {
    int i = texto.indexOf(abre);
    int f = texto.indexOf(fecha, i + 1);
    return (i >= 0 && f > i) ? texto.substring(i + 1, f) : "desconhecidos";
  }

  /**
   * Parâmetro de query ou de rota com valor que não pode ser convertido para o
   * tipo esperado.
   *
   * <p>Exemplo: {@code ?tipo=XYZ}, quando os valores válidos são
   * {@code RECEITA} e {@code DESPESA}.
   */
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

  /** Parâmetro obrigatório não informado na chamada. */
  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErroResponse> tratarParametroAusente(
      MissingServletRequestParameterException ex, HttpServletRequest request) {

    String mensagem =
        "O parâmetro obrigatório '" + ex.getParameterName() + "' não foi informado";

    return erro(HttpStatus.BAD_REQUEST, "Parâmetro ausente", mensagem, request,
        List.of("parâmetro: " + ex.getParameterName() + ", tipo: "
            + ex.getParameterType()));
  }

  // ==================================================================
  // 404 — recurso ou rota inexistente
  // ==================================================================

  @ExceptionHandler(RecursoNaoEncontradoException.class)
  public ResponseEntity<ErroResponse> tratarNaoEncontrado(
      RecursoNaoEncontradoException ex, HttpServletRequest request) {
    return erro(HttpStatus.NOT_FOUND, "Recurso não encontrado", ex.getMessage(), request, List.of());
  }

  /**
   * Rota inexistente.
   *
   * <p>No Spring Boot 4, uma URL que não corresponde a nenhum endpoint é tratada
   * como "recurso estático não encontrado" ({@link NoResourceFoundException}), e
   * não como {@code NoHandlerFoundException}. As duas são tratadas aqui porque a
   * segunda ainda pode ocorrer conforme a configuração.
   */
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

  // ==================================================================
  // 405 — método HTTP não suportado
  // ==================================================================

  /**
   * O verbo HTTP não é aceito pela rota.
   *
   * <p>Exemplo: {@code DELETE /api/v1/movimentacoes/1}. A rota existe, mas uma
   * movimentação é um fato consumado e não pode ser apagada (RN20) — por isso não
   * há {@code DELETE} mapeado.
   */
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

  // ==================================================================
  // 409 — conflito com o estado atual dos dados
  // ==================================================================

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

  /**
   * Violação de integridade detectada pelo <b>banco de dados</b>.
   *
   * <p>É a última linha de defesa. Os services validam antes de tentar gravar, mas
   * entre a validação e o {@code DELETE} alguém pode ter inserido uma linha
   * dependente — e é o banco que recusa. Sem este tratador, o erro viraria um 500
   * e a mensagem do banco seria repassada ao cliente, incluindo o SQL executado:
   *
   * <pre>
   *   could not execute statement [...] SQL [delete from usuario where id_usuario=?]
   *   constraint [fk_categoria_usuario]
   * </pre>
   *
   * <p>Isso expõe estrutura interna sem ajudar quem chamou a API. Aqui o nome da
   * constraint é traduzido para uma frase que explica a relação que bloqueou a
   * operação.
   */
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
    // Violações de unicidade nem sempre citam a constraint.
    return texto.contains("Duplicate entry") ? "restricao de unicidade" : "desconhecida";
  }

  /**
   * Traduz o nome técnico da constraint para uma explicação útil.
   *
   * <p>O nome segue o padrão adotado no projeto ({@code fk_<tabela>_<referencia>} e
   * {@code uq_<tabela>_<colunas>}), o que permite montar a mensagem sem consultar
   * o banco.
   */
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

  // ==================================================================
  // 422 — regra de negócio
  // ==================================================================

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

  // ==================================================================
  // 500 — erro realmente inesperado
  // ==================================================================

  /**
   * Última rede de proteção.
   *
   * <p><b>Só deve ser alcançado por defeitos reais</b> — não por erro do cliente.
   * Se um erro do cliente chegar aqui, é sinal de que falta um tratador
   * específico acima.
   *
   * <p>A mensagem devolvida é <b>deliberadamente genérica</b>: a causa completa é
   * registrada no log do servidor, mas não é enviada ao cliente, porque pode
   * conter SQL, nomes de tabelas e caminhos internos. Quem depurar o problema usa
   * o log; quem consome a API recebe uma resposta honesta e sem vazamento.
   */
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

  // ==================================================================
  // Apoio
  // ==================================================================

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
