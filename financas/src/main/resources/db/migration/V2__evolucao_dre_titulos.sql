-- ============================================================================
--  V2 — Evolução do sistema de finanças pessoais para o modelo de DRE + Títulos
-- ============================================================================
--
--  CONTEXTO
--  --------
--  Este projeto usa `spring.jpa.hibernate.ddl-auto=update`. Isso significa que,
--  ao subir a aplicação, o Hibernate cria tabelas e adiciona colunas que faltam.
--  Porém o Hibernate NÃO faz três coisas:
--
--    1. não remove tabelas que deixaram de existir no código;
--    2. não cria as restrições CHECK que a regra de negócio exige;
--    3. não garante os nomes de índice e chave estrangeira que queremos.
--
--  Por isso este script existe. Ele é o registro explícito da evolução do
--  modelo, útil para o trabalho acadêmico e para reproduzir o banco do zero.
--
--  COMO RODAR
--  ----------
--    mariadb -u root -p financas < V2__evolucao_dre_titulos.sql
--
--  ORDEM DAS OPERAÇÕES
--  -------------------
--  A ordem importa: primeiro removemos as tabelas que foram absorvidas, depois
--  ajustamos a coluna que estava curta demais, depois criamos a tabela nova e,
--  por fim, acrescentamos as colunas e as chaves estrangeiras em `movimentacao`.
--
--  SEGURANÇA
--  ---------
--  Este script foi escrito para um banco com as tabelas VAZIAS (era o estado do
--  projeto). Se você já tiver dados em `recebimento` ou `pagamento`, NÃO execute
--  a seção 1 antes de migrar esses dados para `movimentacao` — veja a seção 6.
-- ============================================================================


-- ============================================================================
--  1. REMOÇÃO DAS TABELAS ABSORVIDAS POR `movimentacao`
-- ============================================================================
--
--  POR QUE REMOVER
--  ---------------
--  `recebimento` e `pagamento` eram tabelas-gêmeas: mesma estrutura
--  (valor, data, movimentacao_id, conta_id), mudando apenas o nome. A diferença
--  "entrada x saída" já está em `movimentacao.tipo`.
--
--  Manter as duas obrigava a aplicação a validar em dois lugares a mesma regra
--  ("pagamento pertence a despesa", "recebimento pertence a receita") e obrigava
--  todo relatório a somar dois totais com UNION. Além disso, existiam DUAS datas
--  e DOIS valores concorrentes para o mesmo dinheiro, sem nada no modelo dizendo
--  qual era a previsão e qual era a realização.
--
--  Agora a conta utilizada vive em `movimentacao.conta_id`, e existe uma única
--  fonte de verdade para o caixa.
--
--  ATENÇÃO: a remoção precisa respeitar a ordem das chaves estrangeiras.
--  `recebimento` e `pagamento` referenciam `movimentacao`; portanto são
--  removidas primeiro (nenhuma outra tabela aponta para elas).

DROP TABLE IF EXISTS recebimento;
DROP TABLE IF EXISTS pagamento;


-- ============================================================================
--  2. CORREÇÃO DO TAMANHO DO NOME DA CATEGORIA
-- ============================================================================
--
--  ESTRUTURA ATUAL -> PROBLEMA -> NOVA ESTRUTURA -> MOTIVO
--  -------------------------------------------------------
--  `categoria.nome` era VARCHAR(10). Com esse limite, categorias absolutamente
--  normais — e que o próprio enunciado do sistema cita — não podiam ser
--  cadastradas:
--
--      "Alimentação"      -> 11 caracteres  (não cabia)
--      "Rendimentos"      -> 11 caracteres  (não cabia)
--      "Outras receitas"  -> 15 caracteres  (não cabia)
--
--  O cadastro falhava com erro de truncamento. Agora são 100 caracteres.
--
--  A restrição UNIQUE(nome, usuario_id) é mantida: o nome da categoria é único
--  por usuário, e não globalmente. Com a coluna maior, essa restrição passa a
--  funcionar como se espera.

ALTER TABLE categoria
    MODIFY COLUMN nome VARCHAR(100) NOT NULL;


-- ============================================================================
--  3. NOVA TABELA `titulo` — O COMPROMISSO FINANCEIRO PREVISTO
-- ============================================================================
--
--  FINALIDADE
--  ----------
--  Registrar aquilo que foi previsto para receber ou pagar, com valor,
--  vencimento e situação, SEM afirmar que o dinheiro já se moveu.
--
--  É a materialização do conceito "Título = promessa / Movimentação =
--  cumprimento". Sem esta tabela, cadastrar uma conta a pagar criaria
--  imediatamente uma despesa, que é exatamente o que se quer evitar.
--
--  CARDINALIDADE
--  -------------
--      usuario      1 : N  titulo
--      categoria    1 : N  titulo
--      titulo       1 : N  movimentacao   (0..N — pode nunca ser quitado)
--
--  SOBRE A COLUNA `situacao`
--  -------------------------
--  Guarda apenas PENDENTE, PAGO e CANCELADO. "VENCIDO" NÃO é armazenado: é
--  derivado em tempo de leitura (PENDENTE + data_vencimento < hoje). Se fosse
--  gravado, seria preciso um job agendado varrendo a tabela toda madrugada, e
--  qualquer falha desse job faria a situação mentir.
--
--  SOBRE A AUSÊNCIA DE `conta_id`
--  ------------------------------
--  Um título ainda não pago não está em banco nenhum. A conta só é conhecida no
--  momento do pagamento e, por isso, vive em `movimentacao`. Colocar uma conta
--  aqui afirmaria que a obrigação já saiu de uma conta.

CREATE TABLE IF NOT EXISTS titulo (
    id_titulo       BIGINT         NOT NULL AUTO_INCREMENT,
    descricao       VARCHAR(255)   NOT NULL,
    valor_previsto  DECIMAL(15,2)  NOT NULL,
    data_vencimento DATE           NOT NULL,
    data_pagamento  DATE           NULL,
    tipo            ENUM('RECEITA','DESPESA') NOT NULL,
    situacao        ENUM('PENDENTE','PAGO','CANCELADO') NOT NULL DEFAULT 'PENDENTE',
    observacao      VARCHAR(255)   NULL,
    categoria_id    BIGINT         NOT NULL,
    usuario_id      BIGINT         NOT NULL,

    PRIMARY KEY (id_titulo),

    -- Categoria e usuário com títulos não podem ser apagados: RESTRICT é o
    -- comportamento padrão, declarado aqui para ficar explícito.
    CONSTRAINT fk_titulo_categoria
        FOREIGN KEY (categoria_id) REFERENCES categoria (id_categoria)
        ON DELETE RESTRICT ON UPDATE RESTRICT,

    CONSTRAINT fk_titulo_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario (id_usuario)
        ON DELETE RESTRICT ON UPDATE RESTRICT,

    -- RN01: não existe obrigação de valor zero ou negativo.
    CONSTRAINT ck_titulo_valor_previsto_positivo
        CHECK (valor_previsto > 0),

    -- RN10: não existe título quitado sem data de quitação. Esta é a garantia
    -- de que "PAGO" sempre terá uma data que o explique.
    CONSTRAINT ck_titulo_pago_tem_data
        CHECK (situacao <> 'PAGO' OR data_pagamento IS NOT NULL)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Índices que espelham as consultas reais do sistema.
-- A DRE prevista filtra por (usuario, vencimento); a listagem de vencidos
-- filtra por (usuario, situacao).
CREATE INDEX idx_titulo_usuario_vencimento ON titulo (usuario_id, data_vencimento);
CREATE INDEX idx_titulo_usuario_situacao   ON titulo (usuario_id, situacao);


-- ============================================================================
--  4. ALTERAÇÃO DA TABELA `movimentacao` — O EVENTO FINANCEIRO REALIZADO
-- ============================================================================
--
--  MUDANÇA DE SIGNIFICADO
--  ----------------------
--  Antes, `movimentacao` era ambígua: tinha valor e data E AINDA era o lado "1"
--  de uma relação com recebimento/pagamento, que tinham outro valor e outra
--  data. Não havia como saber qual era a previsão e qual era a realização.
--
--  Agora seus campos são inequívocos:
--
--      valor  -> valor REALIZADO (o que de fato se moveu)
--      data   -> data do EVENTO    (quando de fato aconteceu)
--
--  DUAS COLUNAS NOVAS
--  ------------------
--    conta_id  (obrigatória) — absorve o papel de recebimento/pagamento.
--                              Todo evento financeiro acontece em alguma conta,
--                              e é isso que torna o cálculo de saldo uma
--                              consulta única, sem UNION.
--    titulo_id (opcional)    — liga a realização à previsão que a originou.
--                              Fica NULL em lançamentos avulsos, como "paguei
--                              um café em dinheiro", que nunca tiveram título.
--                              É essa opcionalidade que garante que nada do
--                              funcionamento anterior se perde.
--
--  NOTA SOBRE `conta_id NOT NULL`
--  ------------------------------
--  A coluna é criada já como NOT NULL porque o banco está vazio. Isso a mantém
--  idêntica ao que o Hibernate gera a partir da entidade (onde `conta` é
--  `optional = false`), evitando que o `ddl-auto=update` altere a coluna na
--  próxima subida da aplicação.
--
--  Se você tivesse dados antigos em `movimentacao` (sem conta), a sequência
--  correta seria: criar a coluna como NULL, migrar os dados preenchendo
--  `conta_id`, e só então aplicar o NOT NULL. A seção 5 traz o comando.

ALTER TABLE movimentacao
    ADD COLUMN conta_id  BIGINT NOT NULL AFTER categoria_id,
    ADD COLUMN titulo_id BIGINT NULL AFTER conta_id;

-- As chaves estrangeiras vêm em instruções separadas para que o script possa
-- ser reexecutado sem erro caso a coluna já exista.
ALTER TABLE movimentacao
    ADD CONSTRAINT fk_movimentacao_conta
        FOREIGN KEY (conta_id) REFERENCES conta (id_conta)
        ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE movimentacao
    ADD CONSTRAINT fk_movimentacao_titulo
        FOREIGN KEY (titulo_id) REFERENCES titulo (id_titulo)
        ON DELETE RESTRICT ON UPDATE RESTRICT;

-- RN13: o sinal (entrada/saída) vem do campo `tipo`, nunca de um valor negativo.
-- Um CHECK aqui substitui, no banco, a validação @Positive feita na aplicação.
ALTER TABLE movimentacao
    ADD CONSTRAINT ck_movimentacao_valor_positivo
        CHECK (valor > 0);

CREATE INDEX idx_movimentacao_usuario_data ON movimentacao (usuario_id, data);
CREATE INDEX idx_movimentacao_conta        ON movimentacao (conta_id);
CREATE INDEX idx_movimentacao_titulo       ON movimentacao (titulo_id);


-- ============================================================================
--  5. (OPCIONAL) TORNAR `conta_id` OBRIGATÓRIA EM BANCO COM DADOS
-- ============================================================================
--
--  Em um banco vazio, `conta_id` já foi criada como NOT NULL na seção 4.
--
--  Este bloco só é necessário se você adaptou a seção 4 para criar a coluna como
--  NULL (o caminho obrigatório quando existem linhas antigas sem conta). Depois
--  de preencher `conta_id` em todas as linhas, aplique:
--
-- ALTER TABLE movimentacao MODIFY COLUMN conta_id BIGINT NOT NULL;


-- ============================================================================
--  6. (OPCIONAL) MIGRAÇÃO DE DADOS DE recebimento / pagamento
-- ============================================================================
--
--  NÃO é necessário para este projeto, porque as tabelas estavam vazias.
--  Fica registrado como referência de como seria uma migração com dados reais.
--
--  A ideia: cada linha de `pagamento` vira uma movimentação de DESPESA, e cada
--  linha de `recebimento` vira uma movimentação de RECEITA. Como
--  `movimentacao` já possuía valor/data próprios, é preciso decidir qual valor
--  prevalece — e a resposta correta é o valor EFETIVAMENTE pago/recebido, que
--  está justamente em pagamento/recebimento.
--
--  UPDATE movimentacao m
--     JOIN pagamento p ON p.movimentacao_id = m.id_movimentacao
--     SET m.valor    = p.valor,
--         m.data     = p.data_pagamento,
--         m.conta_id = p.conta_id
--   WHERE m.tipo = 'DESPESA';
--
--  UPDATE movimentacao m
--     JOIN recebimento r ON r.movimentacao_id = m.id_movimentacao
--     SET m.valor    = r.valor,
--         m.data     = r.data_recebimento,
--         m.conta_id = r.conta_id
--   WHERE m.tipo = 'RECEITA';
--
--  Depois disso, os títulos correspondentes poderiam ser criados com
--  situacao = 'PAGO' e data_pagamento preenchida, e as movimentações
--  apontariam para eles via `titulo_id`.


-- ============================================================================
--  7. CONFERÊNCIA
-- ============================================================================
--
--  Estrutura final esperada: CINCO tabelas.
--
--      usuario
--      conta
--      categoria      (nome agora VARCHAR(100))
--      titulo         (nova)
--      movimentacao   (com conta_id e titulo_id)
--
--  Consulte com:
--
--      SHOW TABLES;
--      SHOW CREATE TABLE titulo;
--      SHOW CREATE TABLE movimentacao;
-- ============================================================================
