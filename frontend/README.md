# Front-end — Gestão de Finanças Pessoais

Interface web que consome a API REST em `financas/` (Spring Boot). Não implementa regras próprias: todas as operações e cálculos (saldos, situação dos títulos, DRE, posição de contas) vêm do back-end.

## Tecnologias

Next.js 16 (App Router) · TypeScript · Tailwind CSS 4 · shadcn/ui (Radix) · lucide-react · TanStack Query · React Hook Form + Zod · Recharts

## Como executar

1. Suba a API (porta `8080`), conforme o README da raiz.
2. Instale as dependências e inicie o front-end:

```bash
cd frontend
npm install
npm run dev
```

A aplicação abre em `http://localhost:5173`, origem já liberada no CORS da API. As chamadas a `/api/v1/*` são encaminhadas para a API pelo próprio Next.js. Para usar outro endereço da API, defina `API_URL` (padrão `http://localhost:8080`).

## Telas e endpoints consumidos

| Tela | Endpoints |
|---|---|
| Entrar / Cadastro | `POST /auth/login`, `POST /usuarios`, `GET /usuarios/me` |
| Painel | `GET /resumo-financeiro`, `GET /contas`, `GET /titulos/vencidos`, `GET /movimentacoes` |
| Contas | `GET/POST/PUT/DELETE /contas`, `POST /movimentacoes/transferir` |
| Extrato | `GET /contas/{id}`, `GET /contas/{id}/extrato`, `POST /movimentacoes`, `POST /movimentacoes/{id}/estornar` |
| Movimentações | `GET /movimentacoes`, `GET /movimentacoes/{id}`, `POST /movimentacoes`, `POST /movimentacoes/transferir`, `POST /movimentacoes/{id}/estornar` |
| Títulos | `GET /titulos`, `GET /titulos/{id}`, `GET /titulos/vencidos`, `POST/PUT/DELETE /titulos`, `POST /titulos/{id}/pagar`, `POST /titulos/{id}/receber`, `POST /titulos/{id}/cancelar` |
| Contas a pagar / a receber | `GET /contas-a-pagar`, `GET /contas-a-receber` |
| DRE | `GET /dre` (modos previsto, realizado e comparativo) |
| Categorias | `GET/POST/PUT/DELETE /categorias` |
| Perfil | `GET /usuarios/me`, `PUT /usuarios/{id}`, `DELETE /usuarios/{id}` |
