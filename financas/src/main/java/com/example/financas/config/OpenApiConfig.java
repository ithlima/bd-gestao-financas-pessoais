package com.example.financas.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI financasOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Gerenciador de Finanças Pessoais")
                .version("1.0.0")
                .description(
                    """
                    API REST para gestão de finanças pessoais com **DRE adaptada para pessoa física**.

                    ## O conceito central

                    O sistema separa **previsão** de **realização**:

                    | | Título | Movimentação |
                    |---|---|---|
                    | Responde | "o que eu tenho a pagar/receber?" | "o que aconteceu com o meu dinheiro?" |
                    | Natureza | compromisso previsto | evento realizado |
                    | Valor | `valorPrevisto` | `valor` (realizado) |
                    | Data | `dataVencimento` | `data` do evento |
                    | Conta | **não tem** | obrigatória |
                    | Efeito na DRE realizada | **nenhum** | é a **única** fonte |

                    Em uma frase: **o título é a promessa; a movimentação é o cumprimento.**

                    ## Sequência sugerida para testar

                    1. `POST /api/v1/usuarios` — criar um usuário
                    2. `POST /api/v1/contas` — criar uma conta (informe o `usuarioId`)
                    3. `POST /api/v1/categorias` — criar ao menos uma de RECEITA e uma de DESPESA
                    4. `POST /api/v1/titulos` — cadastrar uma previsão
                    5. `GET /api/v1/dre` — conferir que a despesa aparece em **previsto**
                       e **não** em realizado
                    6. `POST /api/v1/titulos/{id}/pagar` — registrar o pagamento
                    7. `GET /api/v1/dre` — conferir que agora aparece em **realizado**
                    8. `GET /api/v1/contas-a-pagar` — ver a posição de contas a pagar

                    ## Formato das datas

                    Todas as datas usam o formato ISO **AAAA-MM-DD** (ex.: `2026-03-10`),
                    tanto na entrada quanto nas respostas.

                    ## Regras de negócio que geram erro

                    A API recusa operações que violam o domínio, com o status adequado:

                    - **400** — JSON malformado, enum ou data inválidos, campo reprovado
                    - **404** — id ou rota inexistente
                    - **405** — método HTTP não aceito pela rota
                    - **409** — e-mail/nome duplicado, ou exclusão bloqueada por vínculos
                    - **422** — regra de negócio: pagar mais que o valor em aberto, quitar
                      título já pago, usar categoria de tipo incompatível, etc.

                    Toda resposta de erro tem o mesmo formato, com `status`, `erro`,
                    `mensagem` e `caminho`.
                    """)
                .contact(new Contact().name("Projeto acadêmico").url("https://github.com/ithlima/bd-gestao-financas-pessoais"))
                .license(new License().name("Projeto acadêmico")))

        .components(new Components())
        .tags(
            List.of(
                new Tag().name("Usuários").description("Cadastro dos donos dos dados financeiros."),
                new Tag().name("Contas").description("Onde o dinheiro está. O saldo é calculado, não armazenado."),
                new Tag().name("Categorias").description("Como o usuário classifica. É a categoria que forma as **linhas da DRE**."),
                new Tag()
                    .name("Títulos")
                    .description(
                        """
                        Os **compromissos previstos** — o que se tem a pagar ou a receber.

                        Cadastrar um título **não** cria despesa: nenhum valor sai ou entra, e a
                        DRE realizada permanece inalterada. A despesa passa a existir quando o
                        título é pago ou recebido.
                        """),
                new Tag()
                    .name("Movimentações")
                    .description(
                        """
                        Os **eventos realizados** — o que de fato aconteceu com o dinheiro.

                        Uma movimentação é um fato consumado: não há edição nem exclusão.
                        Correções exigem estorno e novo lançamento.
                        """),
                new Tag().name("Relatórios").description("DRE (previsto, realizado e comparativo) e a posição de contas a pagar e a receber.")));
  }
}
