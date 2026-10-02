package com.example.financas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes das <b>respostas de erro da API</b>.
 *
 * <p>O objetivo destes testes é garantir a promessa feita ao cliente: nenhuma
 * falha chega como "erro interno" genérico, e nenhuma mensagem vaza detalhe
 * interno (SQL, nome de tabela, classe Java).
 *
 * <p>Cada teste verifica três coisas:
 *
 * <ol>
 *   <li>o <b>status HTTP</b> correto para a situação;</li>
 *   <li>que a <b>mensagem</b> explica o problema em português, sem jargão técnico;</li>
 *   <li>que o corpo segue o <b>formato único</b> {@code ErroResponse}.</li>
 * </ol>
 *
 * <p>Usa {@code MockMvc}, que exercita a pilha real do Spring MVC (inclusive o
 * {@code GlobalExceptionHandler}) sem precisar de servidor de verdade.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "/testdata/10-dados-base.sql")
class TratamentoDeErrosApiTest {

  private static final String TITULOS = "/api/v1/titulos";
  private static final String USUARIOS = "/api/v1/usuarios";
  private static final String CATEGORIAS = "/api/v1/categorias";
  private static final String CONTAS = "/api/v1/contas";

  @Autowired private MockMvc mockMvc;

  // ==================================================================
  // 400 — o cliente enviou algo malformado
  // ==================================================================

  @Test
  @DisplayName("Campo inválido no corpo: 400 com a lista de campos reprovados")
  void campoInvalidoRetorna400() throws Exception {
    String corpo =
        """
        {"descricao":"Conta","valorPrevisto":0,"dataVencimento":"2026-03-10",
         "tipo":"DESPESA","categoriaId":1,"usuarioId":1}
        """;

    mockMvc
        .perform(post(TITULOS).contentType(MediaType.APPLICATION_JSON).content(corpo))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.erro").value("Erro de validação"))
        .andExpect(jsonPath("$.detalhes[0]").value(
            "valorPrevisto: O valor previsto deve ser maior que zero"));
  }

  @Test
  @DisplayName("Vários campos inválidos: 400 devolve todos de uma vez")
  void variosCamposInvalidosRetornaTodos() throws Exception {
    // descricao em branco, valorPrevisto nulo, dataVencimento nula
    String corpo =
        """
        {"descricao":"","valorPrevisto":null,"dataVencimento":null,
         "tipo":"DESPESA","categoriaId":1,"usuarioId":1}
        """;

    mockMvc
        .perform(post(TITULOS).contentType(MediaType.APPLICATION_JSON).content(corpo))
        .andExpect(status().isBadRequest())
        // O cliente deve receber todos os problemas em uma única resposta,
        // e não descobri-los um a um a cada tentativa.
        .andExpect(jsonPath("$.detalhes.length()").value(3));
  }

  @Test
  @DisplayName("JSON malformado: 400 explicando que o JSON está inválido")
  void jsonMalformadoRetorna400() throws Exception {
    mockMvc
        .perform(
            post(TITULOS)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"descricao\": \"Conta\", "))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.mensagem").value(
            "O JSON enviado está malformado. Verifique chaves, aspas e vírgulas"));
  }

  @Test
  @DisplayName("Enum inválido: 400 listando os valores aceitos")
  void enumInvalidoRetorna400() throws Exception {
    String corpo =
        """
        {"descricao":"Conta","valorPrevisto":10,"dataVencimento":"2026-03-10",
         "tipo":"INVALIDO","categoriaId":1,"usuarioId":1}
        """;

    mockMvc
        .perform(post(TITULOS).contentType(MediaType.APPLICATION_JSON).content(corpo))
        .andExpect(status().isBadRequest())
        // A mensagem crua do Jackson citaria a classe Java do enum. O cliente
        // precisa saber quais valores PODE enviar.
        .andExpect(jsonPath("$.mensagem").value(
            "Valor inválido para um campo de opções. Valores aceitos: RECEITA, DESPESA"));
  }

  @Test
  @DisplayName("Corpo ausente: 400 dizendo que o corpo é obrigatório")
  void corpoAusenteRetorna400() throws Exception {
    mockMvc
        .perform(post(TITULOS).contentType(MediaType.APPLICATION_JSON).content(""))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.mensagem").value(
            "O corpo da requisição é obrigatório e não foi enviado"));
  }

  @Test
  @DisplayName("Data em formato errado: 400 indicando o formato correto")
  void dataInvalidaRetorna400() throws Exception {
    String corpo =
        """
        {"descricao":"Conta","valorPrevisto":10,"dataVencimento":"31/02/2026",
         "tipo":"DESPESA","categoriaId":1,"usuarioId":1}
        """;

    mockMvc
        .perform(post(TITULOS).contentType(MediaType.APPLICATION_JSON).content(corpo))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.mensagem").value(
            "Data inválida. Use o formato ISO: AAAA-MM-DD (exemplo: 2026-03-10)"));
  }

  @Test
  @DisplayName("Parâmetro de query com tipo inválido: 400 listando os valores aceitos")
  void parametroDeTipoInvalidoRetorna400() throws Exception {
    mockMvc
        .perform(get(TITULOS).param("usuarioId", "1").param("tipo", "XYZ"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.erro").value("Parâmetro inválido"))
        .andExpect(jsonPath("$.mensagem").value(
            "O parâmetro 'tipo' recebeu um valor inválido. Valores aceitos: RECEITA, DESPESA"));
  }

  @Test
  @DisplayName("Data em query com formato errado: 400 indicando o formato correto")
  void dataEmQueryInvalidaRetorna400() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/dre")
                .param("usuarioId", "1")
                .param("inicio", "01/03/2026")
                .param("fim", "31/03/2026"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.mensagem").value(
            "O parâmetro 'inicio' recebeu um valor inválido. "
                + "Use o formato ISO: AAAA-MM-DD (exemplo: 2026-03-10)"));
  }

  @Test
  @DisplayName("Parâmetro obrigatório ausente: 400 dizendo qual faltou")
  void parametroAusenteRetorna400() throws Exception {
    mockMvc
        .perform(get(TITULOS))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.erro").value("Parâmetro ausente"))
        .andExpect(jsonPath("$.mensagem").value(
            "O parâmetro obrigatório 'usuarioId' não foi informado"));
  }

  // ==================================================================
  // 404 — recurso ou rota inexistente
  // ==================================================================

  @Test
  @DisplayName("Id inexistente: 404 com mensagem clara")
  void idInexistenteRetorna404() throws Exception {
    mockMvc
        .perform(get(USUARIOS + "/99999"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.erro").value("Recurso não encontrado"))
        .andExpect(jsonPath("$.mensagem").value(
            "Usuário não encontrado para o id 99999"));
  }

  @Test
  @DisplayName("Rota inexistente: 404 (não 500) dizendo qual rota não existe")
  void rotaInexistenteRetorna404() throws Exception {
    mockMvc
        .perform(get("/api/v1/rota-que-nao-existe"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.erro").value("Rota não encontrada"))
        .andExpect(jsonPath("$.mensagem").value(
            "Não existe endpoint para GET /api/v1/rota-que-nao-existe"));
  }

  // ==================================================================
  // 405 — método HTTP não suportado
  // ==================================================================

  @Test
  @DisplayName("Método não aceito na rota: 405 listando os métodos aceitos")
  void metodoNaoSuportadoRetorna405() throws Exception {
    // Uma movimentação é um fato consumado: não há DELETE mapeado (RN20).
    mockMvc
        .perform(delete("/api/v1/movimentacoes/1"))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(jsonPath("$.status").value(405))
        .andExpect(jsonPath("$.erro").value("Método não permitido"))
        .andExpect(jsonPath("$.mensagem").value(
            "O método DELETE não é aceito nesta rota. Métodos aceitos: GET"));
  }

  @Test
  @DisplayName("Verbo não mapeado em qualquer rota: 405")
  void verboNaoMapeadoRetorna405() throws Exception {
    mockMvc
        .perform(patch(USUARIOS + "/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(jsonPath("$.erro").value("Método não permitido"));
  }

  // ==================================================================
  // 409 — conflito com o estado dos dados
  // ==================================================================

  @Test
  @DisplayName("E-mail duplicado: 409")
  void emailDuplicadoRetorna409() throws Exception {
    String corpo =
        """
        {"nome":"Outra","email":"ana@teste.com","senha":"segredo123"}
        """;

    mockMvc
        .perform(post(USUARIOS).contentType(MediaType.APPLICATION_JSON).content(corpo))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.erro").value("Registro duplicado"))
        .andExpect(jsonPath("$.mensagem").value(
            "Já existe um usuário com o e-mail ana@teste.com"));
  }

  @Test
  @DisplayName("Excluir categoria em uso: 409 informando quantos títulos a usam")
  void excluirCategoriaEmUsoRetorna409() throws Exception {
    // Ana (usuário 1) passa a ter um título na categoria Moradia (id 1).
    String corpo =
        """
        {"descricao":"Aluguel","valorPrevisto":1200,"dataVencimento":"2026-03-10",
         "tipo":"DESPESA","categoriaId":1,"usuarioId":1}
        """;
    mockMvc
        .perform(post(TITULOS).contentType(MediaType.APPLICATION_JSON).content(corpo))
        .andExpect(status().isCreated());

    mockMvc
        .perform(delete(CATEGORIAS + "/1"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.erro").value("Recurso em uso"))
        // A mensagem diz QUANTOS registros impedem a exclusão — informação que
        // o erro cru do banco não daria.
        .andExpect(jsonPath("$.mensagem").value(
            "Não dá para excluir categoria porque há 1 título vinculado a ele"));
  }

  @Test
  @DisplayName("Excluir conta com movimentação: 409")
  void excluirContaComMovimentacaoRetorna409() throws Exception {
    // Título quitado gera uma movimentação na conta 1.
    String titulo =
        """
        {"descricao":"Aluguel","valorPrevisto":1200,"dataVencimento":"2026-03-10",
         "tipo":"DESPESA","categoriaId":1,"usuarioId":1}
        """;
    String localizacao =
        mockMvc
            .perform(post(TITULOS).contentType(MediaType.APPLICATION_JSON).content(titulo))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    Long idTitulo = extrairId(localizacao);

    String pagamento = """
        {"valor":1200,"data":"2026-03-08","contaId":1}
        """;
    mockMvc
        .perform(
            post(TITULOS + "/" + idTitulo + "/pagar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(pagamento))
        .andExpect(status().isOk());

    mockMvc
        .perform(delete(CONTAS + "/1"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.erro").value("Recurso em uso"))
        .andExpect(jsonPath("$.mensagem").value(
            "Não dá para excluir conta porque há 1 movimentação vinculada a ele"));
  }

  @Test
  @DisplayName("Excluir usuário com dados vinculados: 409 dizendo o que depende dele")
  void excluirUsuarioComDadosRetorna409() throws Exception {
    // Ana tem uma conta ("Conta da Ana") e três categorias, mas nenhum título.
    //
    // A verificação é feita em ordem de relevância — títulos, movimentações,
    // contas, categorias — e para na primeira que encontrar. Como não há títulos
    // nem movimentações, o bloqueio relatado é a conta. A ordem importa: se todas
    // as dependências tivessem a mesma prioridade, a mensagem poderia citar
    // "3 categorias" quando o dado mais relevante é a conta.
    mockMvc
        .perform(delete(USUARIOS + "/1"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.erro").value("Recurso em uso"))
        .andExpect(jsonPath("$.mensagem").value(
            "Não dá para excluir usuário porque há 1 conta vinculada a ele"));
  }

  // ==================================================================
  // 422 — regra de negócio
  // ==================================================================

  @Test
  @DisplayName("Regra de negócio violada: 422 com a explicação do domínio")
  void regraDeNegocioRetorna422() throws Exception {
    // Categoria "Moradia" é de DESPESA; usá-la em uma RECEITA viola a RN03.
    String corpo =
        """
        {"descricao":"Freelance","valorPrevisto":800,"dataVencimento":"2026-03-25",
         "tipo":"RECEITA","categoriaId":1,"usuarioId":1}
        """;

    mockMvc
        .perform(post(TITULOS).contentType(MediaType.APPLICATION_JSON).content(corpo))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.status").value(422))
        .andExpect(jsonPath("$.erro").value("Regra de negócio violada"))
        .andExpect(jsonPath("$.mensagem").value(
            "A categoria 'Moradia' é do tipo DESPESA e não pode ser usada em título de tipo RECEITA"));
  }

  // ==================================================================
  // Formato e segurança das respostas de erro
  // ==================================================================

  @Test
  @DisplayName("Todo erro segue o mesmo formato, com os mesmos campos")
  void todoErroTemOMesmoFormato() throws Exception {
    mockMvc
        .perform(get("/api/v1/rota-inexistente"))
        .andExpect(jsonPath("$.timestamp").exists())
        .andExpect(jsonPath("$.status").exists())
        .andExpect(jsonPath("$.erro").exists())
        .andExpect(jsonPath("$.mensagem").exists())
        .andExpect(jsonPath("$.caminho").value("/api/v1/rota-inexistente"))
        .andExpect(jsonPath("$.detalhes").isArray());
  }

  @Test
  @DisplayName("Erro de constraint do banco não vaza o SQL executado")
  void erroDeBancoNaoVazaSql() throws Exception {
    // Excluir o usuário 1 esbarra em chave estrangeira. Mesmo que a validação do
    // service não existisse, o datasource recusaria — e a resposta NÃO pode
    // conter o SQL nem o nome da constraint.
    String resposta =
        mockMvc
            .perform(delete(USUARIOS + "/1"))
            .andExpect(status().isConflict())
            .andReturn()
            .getResponse()
            .getContentAsString();

    org.junit.jupiter.api.Assertions.assertFalse(
        resposta.toLowerCase().contains("sql"),
        "A resposta não pode conter o SQL executado: " + resposta);
    org.junit.jupiter.api.Assertions.assertFalse(
        resposta.contains("fk_"),
        "A resposta não pode expor nome de constraint: " + resposta);
    org.junit.jupiter.api.Assertions.assertFalse(
        resposta.contains("could not execute"),
        "A resposta não pode conter mensagem crua do driver: " + resposta);
  }

  @Test
  @DisplayName("Erro de parsing não vaza detalhe interno do Jackson")
  void erroDeParsingNaoVazaDetalheInterno() throws Exception {
    // A mensagem crua do Jackson traz deslocamento de byte, nome da feature de
    // configuração e a classe Java de destino. Nada disso ajuda quem consome a
    // API, e revela estrutura interna.
    String[] corposProblematicos = {
      "{\"descricao\": \"Conta\", ",                       // JSON truncado
      "{\"descricao\":\"C\",\"valorPrevisto\":1,\"dataVencimento\":\"2026-03-10\","
          + "\"tipo\":\"INVALIDO\",\"categoriaId\":1,\"usuarioId\":1}", // enum inválido
      "{\"descricao\":\"C\",\"valorPrevisto\":1,\"dataVencimento\":\"31/02/2026\","
          + "\"tipo\":\"DESPESA\",\"categoriaId\":1,\"usuarioId\":1}"    // data inválida
    };

    for (String corpo : corposProblematicos) {
      String resposta =
          mockMvc
              .perform(post(TITULOS).contentType(MediaType.APPLICATION_JSON).content(corpo))
              .andExpect(status().isBadRequest())
              .andReturn()
              .getResponse()
              .getContentAsString();

      org.junit.jupiter.api.Assertions.assertFalse(
          resposta.contains("byte offset"),
          "A resposta não pode conter deslocamento de byte: " + resposta);
      org.junit.jupiter.api.Assertions.assertFalse(
          resposta.contains("StreamReadFeature") || resposta.contains("REDACTED"),
          "A resposta não pode expor configuração interna do parser: " + resposta);
      org.junit.jupiter.api.Assertions.assertFalse(
          resposta.contains("com.example.financas"),
          "A resposta não pode expor nomes de classes da aplicação: " + resposta);
      org.junit.jupiter.api.Assertions.assertFalse(
          resposta.contains("Jackson") || resposta.contains("jackson"),
          "A resposta não deve citar a biblioteca de parsing: " + resposta);
    }
  }

  /** Extrai o {@code idTitulo} do JSON devolvido na criação. */
  private Long extrairId(String json) {
    java.util.regex.Matcher m =
        java.util.regex.Pattern.compile("\"idTitulo\"\\s*:\\s*(\\d+)").matcher(json);
    if (!m.find()) {
      throw new IllegalStateException("Não foi possível extrair idTitulo de: " + json);
    }
    return Long.valueOf(m.group(1));
  }
}
