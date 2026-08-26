# 💰 Sistema de Gestão de Finanças Pessoais (API REST)

## 📖 Sobre o Projeto
Este projeto foi desenvolvido como trabalho acadêmico para a disciplina de Banco de Dados. Trata-se de uma API RESTful construída para o gerenciamento de finanças pessoais, permitindo que os usuários tenham controle total sobre suas receitas e despesas.

O foco principal do desenvolvimento foi a estruturação de um banco de dados relacional robusto, aplicando conceitos de modelagem, normalização e integridade referencial, e integrando-o a um backend moderno.

## 🎯 Funcionalidades Principais
* **Gestão de Usuários:** Cadastro e autenticação de usuários.
* **Controle de Contas:** Criação de diferentes contas bancárias ou carteiras para um mesmo usuário.
* **Categorização:** Organização de transações por categorias (ex: Alimentação, Transporte, Salário).
* **Registro de Transações:** Lançamento de receitas e despesas, associadas a contas específicas e categorias.
* **Extrato e Consolidação:** Consultas de saldo atual e histórico de movimentações.

## 🛠️ Tecnologias e Ferramentas Utilizadas
* **Linguagem:** Java 17+
* **Framework Backend:** Spring Boot (Spring Web, Spring Data JPA)
* **Banco de Dados:** MySQL
* **Mapeamento Objeto-Relacional (ORM):** Hibernate
* **Gerenciador de Dependências:** Maven

## 🗄️ Arquitetura do Banco de Dados (MySQL)

A modelagem de dados foi desenhada para garantir a consistência das movimentações financeiras. O esquema principal conta com as seguintes tabelas:

* **`tb_usuarios` (Users):** Armazena os dados dos donos das contas.
* **`tb_contas` (Accounts):** Relacionada aos usuários (1:N). Representa os locais onde o dinheiro está armazenado.
* **`tb_categorias` (Categories):** Tabelas de domínio para classificar as despesas/receitas.
* **`tb_transacoes` (Transactions):** A tabela central do sistema. Possui chaves estrangeiras apontando para a conta e para a categoria (N:1), e registra o valor, tipo (receita ou despesa) e data de cada movimentação.


## 🔗 Estrutura da API (Endpoints Principais)
A comunicação com o banco de dados é feita através de requisições HTTP, mediadas pelo Spring Data JPA. Alguns dos principais endpoints incluem:

* `GET /api/transacoes` - Retorna o histórico de movimentações (com filtros por data/categoria).
* `POST /api/transacoes` - Registra uma nova transação no banco.
* `GET /api/contas/{id}/saldo` - Calcula o saldo atual cruzando receitas e despesas.