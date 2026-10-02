-- ============================================================================
--  Dados de carga dos testes
-- ============================================================================
--
--  Executado pelo @Sql de cada classe de teste, DEPOIS que o Hibernate criou o
--  schema (ddl-auto=create-drop). Serve para começar cada teste de um estado
--  conhecido, em vez de o teste ter de cadastrar tudo o que precisa.
--
--  É IDEMPOTENTE de propósito: começa limpando tudo e recriando com ids fixos.
--  Isso é necessário porque as classes de teste têm comportamentos diferentes:
--
--    * RegrasFinanceirasIntegrationTest é @Transactional, então cada teste
--      desfaz o que fez e o banco volta ao estado da carga;
--    * TratamentoDeErrosApiTest NÃO é transacional (precisa de COMMIT de verdade
--      para exercitar a pilha HTTP completa), então os dados permanecem entre os
--      testes. Sem a limpeza inicial, o segundo teste falharia com chave
--      duplicada.
--
--  O conjunto é proposital e pequeno:
--
--    usuario   : 1 (Ana), 2 (Bruno)  -> o 2 existe para testar isolamento
--    conta     : 1 (Ana), 2 (Bruno)
--    categoria : 1 Moradia   (DESPESA, Ana)
--                2 Salarios  (RECEITA, Ana)
--                3 Lazer     (DESPESA, Ana)
--                4 Outros    (DESPESA, Bruno)
--
--  Não há títulos nem movimentações: cada teste cria o que precisa, para deixar
--  explícito o que está sendo verificado.
--
--  A ordem da limpeza respeita as chaves estrangeiras: primeiro as tabelas que
--  referenciam, depois as referenciadas.
-- ============================================================================

DELETE FROM movimentacao;
DELETE FROM titulo;
DELETE FROM categoria;
DELETE FROM conta;
DELETE FROM usuario;

-- Reinicia os contadores de auto-incremento. Sem isso, um teste não-transacional
-- criaria a movimentação 1, o próximo criaria a 2, e assim por diante: os ids
-- mudariam conforme a ordem de execução e nenhum teste seria repetível de forma
-- isolada.
ALTER TABLE movimentacao AUTO_INCREMENT = 1;
ALTER TABLE titulo AUTO_INCREMENT = 1;
ALTER TABLE categoria AUTO_INCREMENT = 1;
ALTER TABLE conta AUTO_INCREMENT = 1;
ALTER TABLE usuario AUTO_INCREMENT = 1;

INSERT INTO usuario (id_usuario, ativo, data_cadastro, email, nome, senha)
VALUES (1, b'1', NOW(), 'ana@teste.com', 'Ana', 'segredo123');

INSERT INTO usuario (id_usuario, ativo, data_cadastro, email, nome, senha)
VALUES (2, b'1', NOW(), 'bruno@teste.com', 'Bruno', 'segredo123');

INSERT INTO conta (id_conta, nome, saldo_inicial, tipo, usuario_id)
VALUES (1, 'Conta da Ana', 1000.00, 'CORRENTE', 1);

INSERT INTO conta (id_conta, nome, saldo_inicial, tipo, usuario_id)
VALUES (2, 'Conta do Bruno', 500.00, 'CORRENTE', 2);

INSERT INTO categoria (id_categoria, nome, tipo, usuario_id)
VALUES (1, 'Moradia', 'DESPESA', 1);

INSERT INTO categoria (id_categoria, nome, tipo, usuario_id)
VALUES (2, 'Salarios', 'RECEITA', 1);

INSERT INTO categoria (id_categoria, nome, tipo, usuario_id)
VALUES (3, 'Lazer', 'DESPESA', 1);

INSERT INTO categoria (id_categoria, nome, tipo, usuario_id)
VALUES (4, 'Outros', 'DESPESA', 2);
