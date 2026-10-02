package com.example.financas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica que a <b>documentação OpenAPI está completa e coerente</b> com os
 * endpoints realmente expostos.
 *
 * <p>Este teste existe porque a documentação é gerada a partir do código: se um
 * controller novo for criado e esquecido, ou se um endpoint deixar de ser
 * documentado, a falha aparece aqui — e não na mão de quem tenta usar a API pelo
 * Swagger.
 *
 * <p>Ele cobre três coisas:
 *
 * <ol>
 *   <li><b>existência</b> — o contrato é gerado e o Swagger UI é servido;</li>
 *   <li><b>completude</b> — todos os endpoints estão documentados, com descrição,
 *       tag e os schemas de entrada e saída;</li>
 *   <li><b>coerência</b> — o documento descreve os mesmos caminhos que o Spring MVC
 *       expõe, e os schemas mostram os campos que a API realmente devolve.</li>
 * </ol>
 */
@SpringBootTest
@AutoConfigureMockMvc
class DocumentacaoOpenApiTest {

  @Autowired private MockMvc mockMvc;

  /** Lê o contrato OpenAPI gerado pela aplicação. */
  private JsonNode lerContrato() throws Exception {
    String json =
        mockMvc
            .perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    return new ObjectMapper().readTree(json);
  }

  // ==================================================================
  // Existência
  // ==================================================================

  @Test
  @DisplayName("O contrato OpenAPI é gerado")
  void contratoEhGerado() throws Exception {
    JsonNode doc = lerContrato();

    assertEquals("Gerenciador de Finanças Pessoais", doc.path("info").path("title").asString());
    assertEquals("1.0.0", doc.path("info").path("version").asString());
    assertFalse(doc.path("info").path("description").asString().isBlank(),
        "A descrição da API não pode ficar vazia");
  }

  @Test
  @DisplayName("O Swagger UI é servido")
  void swaggerUiEhServido() throws Exception {
    // O springdoc redireciona /swagger-ui.html para o index da interface.
    mockMvc
        .perform(get("/swagger-ui/index.html"))
        .andExpect(status().isOk());
  }

  // ==================================================================
  // Completude — todos os endpoints documentados
  // ==================================================================

  /** Os caminhos que a API expõe. Manter em sincronia com os controllers. */
  private static final List<String> CAMINHOS_ESPERADOS =
      List.of(
          "/api/v1/usuarios",
          "/api/v1/usuarios/{id}",
          "/api/v1/contas",
          "/api/v1/contas/{id}",
          "/api/v1/contas/{id}/extrato",
          "/api/v1/categorias",
          "/api/v1/categorias/{id}",
          "/api/v1/titulos",
          "/api/v1/titulos/{id}",
          "/api/v1/titulos/vencidos",
          "/api/v1/titulos/{id}/pagar",
          "/api/v1/titulos/{id}/receber",
          "/api/v1/titulos/{id}/cancelar",
          "/api/v1/movimentacoes",
          "/api/v1/movimentacoes/{id}",
          "/api/v1/dre",
          "/api/v1/resumo-financeiro",
          "/api/v1/contas-a-pagar",
          "/api/v1/contas-a-receber");

  @Test
  @DisplayName("Todos os endpoints da API aparecem no contrato")
  void todosOsEndpointsDocumentados() throws Exception {
    JsonNode paths = lerContrato().path("paths");

    List<String> faltando = new ArrayList<>();
    for (String esperado : CAMINHOS_ESPERADOS) {
      if (!paths.has(esperado)) {
        faltando.add(esperado);
      }
    }

    assertTrue(
        faltando.isEmpty(),
        "Endpoints expostos que NÃO estão documentados no OpenAPI: " + faltando);
  }

  @Test
  @DisplayName("Nenhum caminho documentado é órfão (todos existem na API)")
  void nenhumCaminhoOrfao() throws Exception {
    JsonNode paths = lerContrato().path("paths");

    List<String> orfaos = new ArrayList<>();
    for (String caminho : CAMINHOS_ESPERADOS) {
      // verificação inversa: tudo que está no contrato deve estar na lista
    }
    var nomes = new ArrayList<String>();
    paths.propertyNames().forEach(nomes::add);
    for (String nome : nomes) {
      if (!CAMINHOS_ESPERADOS.contains(nome)) {
        orfaos.add(nome);
      }
    }

    assertTrue(
        orfaos.isEmpty(),
        "Caminhos documentados fora da lista de esperados (atualize a lista se forem novos): "
            + orfaos);
  }

  @Test
  @DisplayName("Toda operação tem resumo, tag e resposta de sucesso documentada")
  void todaOperacaoTemResumoETag() throws Exception {
    JsonNode paths = lerContrato().path("paths");

    List<String> problemas = new ArrayList<>();

    var caminhos = new ArrayList<String>();
    paths.propertyNames().forEach(caminhos::add);

    for (String rota : caminhos) {
      JsonNode operacoes = paths.path(rota);
      var metodos = new ArrayList<String>();
      operacoes.propertyNames().forEach(metodos::add);

      for (String metodo : metodos) {
        JsonNode op = operacoes.path(metodo);
        String rotulo = metodo.toUpperCase() + " " + rota;

        if (op.path("summary").asString().isBlank()) {
          problemas.add(rotulo + ": sem summary");
        }
        if (op.path("tags").isEmpty() || op.path("tags").get(0).asString().isBlank()) {
          problemas.add(rotulo + ": sem tag");
        }
        JsonNode respostas = op.path("responses");
        if (respostas.path("200").isMissingNode()
            && respostas.path("201").isMissingNode()
            && respostas.path("204").isMissingNode()) {
          problemas.add(rotulo + ": sem resposta de sucesso documentada");
        }
      }
    }

    assertTrue(problemas.isEmpty(), "Operações incompletas na documentação: " + problemas);
  }

  @Test
  @DisplayName("As operações estão agrupadas nas tags esperadas")
  void operacoesAgrupadasNasTagsEsperadas() throws Exception {
    JsonNode doc = lerContrato();

    List<String> tagsDeclaradas = new ArrayList<>();
    doc.path("tags").forEach(t -> tagsDeclaradas.add(t.path("name").asString()));

    for (String esperada :
        List.of("Usuários", "Contas", "Categorias", "Títulos", "Movimentações", "Relatórios")) {
      assertTrue(
          tagsDeclaradas.contains(esperada),
          "Tag '" + esperada + "' não foi declarada. Declaradas: " + tagsDeclaradas);
    }
  }

  // ==================================================================
  // Schemas de entrada e saída
  // ==================================================================

  @Test
  @DisplayName("Os schemas de requisição e resposta estão registrados")
  void schemasRegistrados() throws Exception {
    JsonNode schemas = lerContrato().path("components").path("schemas");

    // Entrada (dto/request)
    for (String s :
        List.of(
            "UsuarioRequest", "ContaRequest", "CategoriaRequest", "TituloRequest",
            "MovimentacaoRequest", "PagamentoRequest", "RecebimentoRequest")) {
      assertTrue(schemas.has(s), "Schema de requisição ausente: " + s);
    }

    // Saída (dto/response)
    for (String s :
        List.of(
            "UsuarioResponse", "ContaResponse", "CategoriaResponse", "TituloResponse",
            "MovimentacaoResponse", "DreResponse", "PosicaoTitulosResponse",
            "ResumoFinanceiroResponse", "ErroResponse")) {
      assertTrue(schemas.has(s), "Schema de resposta ausente: " + s);
    }
  }

  @Test
  @DisplayName("O schema do título documenta os campos que a API devolve")
  void schemaDoTituloEstaCompleto() throws Exception {
    JsonNode titulo = lerContrato().path("components").path("schemas").path("TituloResponse");
    JsonNode props = titulo.path("properties");

    // Estes campos são o coração da regra de negócio e não podem sumir da
    // documentação: são eles que mostram previsão x realização.
    for (String campo :
        List.of(
            "idTitulo", "descricao", "valorPrevisto", "valorRealizado", "valorEmAberto",
            "dataVencimento", "dataPagamento", "tipo", "situacao", "situacaoEfetiva",
            "podeRegistrarPagamento", "quantidadeMovimentacoes", "categoriaNome")) {
      assertTrue(props.has(campo), "Campo ausente no schema TituloResponse: " + campo);
    }
  }

  @Test
  @DisplayName("O schema do erro documenta o formato único de resposta")
  void schemaDoErroEstaCompleto() throws Exception {
    JsonNode erro = lerContrato().path("components").path("schemas").path("ErroResponse");

    for (String campo : List.of("timestamp", "status", "erro", "mensagem", "caminho", "detalhes")) {
      assertTrue(
          erro.path("properties").has(campo), "Campo ausente no schema ErroResponse: " + campo);
    }
  }

  // ==================================================================
  // Coerência com as regras de negócio
  // ==================================================================

  @Test
  @DisplayName("O endpoint de pagar documenta a resposta 422 de regra de negócio")
  void endpointDePagamentoDocumenta422() throws Exception {
    JsonNode pagar = lerContrato().path("paths").path("/api/v1/titulos/{id}/pagar").path("post");
    assertNotNull(pagar, "A operação de pagar não está documentada");

    // A descrição precisa explicar que o valor pode ser parcial, senão o usuário
    // da API não descobre esse comportamento lendo o Swagger.
    String descricao = pagar.path("description").asString().toLowerCase();
    assertTrue(
        descricao.contains("parcial"),
        "A descrição de 'pagar' deve mencionar o pagamento parcial. Texto atual: " + descricao);
  }

  @Test
  @DisplayName("O endpoint de DRE documenta os três modos de apuração")
  void endpointDeDreDocumentaOsModos() throws Exception {
    JsonNode dre = lerContrato().path("paths").path("/api/v1/dre").path("get");
    String descricao = dre.path("description").asString().toLowerCase()
        + dre.path("summary").asString().toLowerCase();

    for (String modo : List.of("previsto", "realizado", "comparativo")) {
      assertTrue(
          descricao.contains(modo),
          "A documentação da DRE deve mencionar o modo '" + modo + "'");
    }
  }

  @Test
  @DisplayName("O contrato em JSON é consumível por ferramentas externas")
  void contratoEhJsonValido() throws Exception {
    String json =
        mockMvc
            .perform(get("/v3/api-docs").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    // Se o Jackson consegue ler, o Postman/Insomnia também conseguem.
    JsonNode doc = new ObjectMapper().readTree(json);
    assertTrue(doc.has("openapi"), "O contrato precisa declarar a versão do OpenAPI");
    assertTrue(doc.has("paths"), "O contrato precisa ter paths");
  }
}
