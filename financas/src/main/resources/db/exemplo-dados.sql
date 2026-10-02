-- ============================================================================
--  Dados de exemplo para testar o sistema
-- ============================================================================
--
--  Insere um usuario com contas, categorias, titulos e movimentacoes para que
--  a DRE e as telas de contas a pagar/receber ja tenham conteudo.
--
--  Como usar:
--      mariadb -u root -p financas < financas/src/main/resources/db/exemplo-dados.sql
--
--  O script e idempotente: apaga os dados existentes antes de inserir.
--  NAO use em banco com dados reais.
--
--  Cenario do exemplo (data de referencia: marco de 2026)
--  -------------------------------------------------------
--  Ana recebe salario de 5.000 e tem um freelance previsto de 1.200.
--  Paga aluguel de 1.500, mercado de 800, transporte de 300 e tem uma
--  consulta medica de 250 ainda pendente.
--
--  Resultado esperado
--  ------------------
--  DRE PREVISTA (titulos por vencimento em marco/2026)
--      receitas   salario 5.000 + freelance 1.200 = 6.200
--      despesas   aluguel 1.500 + mercado 800 + transporte 300 + consulta 250 = 2.850
--      resultado  6.200 - 2.850 = +3.350
--
--  DRE REALIZADA (movimentacoes por data em marco/2026)
--      receitas   salario 5.000 = 5.000
--      despesas   aluguel 1.500 + mercado 800 + transporte 300 = 2.600
--      resultado  5.000 - 2.600 = +2.400
--
--  CONTAS A PAGAR
--      total em aberto 250 (a consulta medica, ainda pendente)
--
--  A diferenca entre os dois blocos e o ponto do sistema: a consulta de 250
--  aparece como previsao e NAO como despesa realizada, porque nao foi paga.
-- ============================================================================

DELETE FROM movimentacao;
DELETE FROM titulo;
DELETE FROM categoria;
DELETE FROM conta;
DELETE FROM usuario;

ALTER TABLE movimentacao AUTO_INCREMENT = 1;
ALTER TABLE titulo AUTO_INCREMENT = 1;
ALTER TABLE categoria AUTO_INCREMENT = 1;
ALTER TABLE conta AUTO_INCREMENT = 1;
ALTER TABLE usuario AUTO_INCREMENT = 1;

INSERT INTO usuario (id_usuario, ativo, data_cadastro, email, nome, senha)
VALUES (1, b'1', NOW(), 'ana@exemplo.com', 'Ana', 'segredo123');

INSERT INTO conta (id_conta, nome, saldo_inicial, tipo, usuario_id) VALUES
  (1, 'Conta Corrente', 2000.00, 'CORRENTE', 1),
  (2, 'Poupanca',       5000.00, 'POUPANCA', 1);

INSERT INTO categoria (id_categoria, nome, tipo, usuario_id) VALUES
  (1, 'Salarios',     'RECEITA', 1),
  (2, 'Freelances',   'RECEITA', 1),
  (3, 'Moradia',      'DESPESA', 1),
  (4, 'Alimentacao',  'DESPESA', 1),
  (5, 'Transporte',   'DESPESA', 1),
  (6, 'Saude',        'DESPESA', 1);

-- Titulos de RECEITA
INSERT INTO titulo (id_titulo, descricao, valor_previsto, data_vencimento,
                    data_pagamento, tipo, situacao, observacao, categoria_id, usuario_id) VALUES
  (1, 'Salario de marco',   5000.00, '2026-03-05', '2026-03-05', 'RECEITA', 'PAGO',     NULL, 1, 1),
  (2, 'Freelance cliente X',1200.00, '2026-03-20', NULL,         'RECEITA', 'PENDENTE', NULL, 2, 1);

-- Titulos de DESPESA
INSERT INTO titulo (id_titulo, descricao, valor_previsto, data_vencimento,
                    data_pagamento, tipo, situacao, observacao, categoria_id, usuario_id) VALUES
  (3, 'Aluguel',            1500.00, '2026-03-10', '2026-03-10', 'DESPESA', 'PAGO',     NULL, 3, 1),
  (4, 'Mercado do mes',      800.00, '2026-03-15', '2026-03-15', 'DESPESA', 'PAGO',     NULL, 4, 1),
  (5, 'Transporte',          300.00, '2026-03-18', '2026-03-18', 'DESPESA', 'PAGO',     NULL, 5, 1),
  (6, 'Consulta medica',     250.00, '2026-03-25', NULL,         'DESPESA', 'PENDENTE', 'Ainda nao paguei', 6, 1);

-- Movimentacoes: um evento realizado para cada titulo PAGO.
-- O titulo pendente (6) NAO tem movimentacao, e e exatamente isso que faz
-- com que ele apareca na DRE prevista e nao na realizada.
INSERT INTO movimentacao (id_movimentacao, descricao, valor, tipo, data,
                          usuario_id, categoria_id, conta_id, titulo_id) VALUES
  (1, 'Salario de marco',   5000.00, 'RECEITA', '2026-03-05', 1, 1, 1, 1),
  (2, 'Aluguel',            1500.00, 'DESPESA', '2026-03-10', 1, 3, 1, 3),
  (3, 'Mercado do mes',      800.00, 'DESPESA', '2026-03-15', 1, 4, 1, 4),
  (4, 'Transporte',          300.00, 'DESPESA', '2026-03-18', 1, 5, 1, 5);
