"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { ApiError, request } from "@/lib/api";
import { useUsuarioId } from "@/lib/auth";
import type {
  Categoria,
  CategoriaRequest,
  Conta,
  ContaRequest,
  Dre,
  LiquidacaoRequest,
  ModoDre,
  Movimentacao,
  MovimentacaoRequest,
  PosicaoTitulos,
  ResumoFinanceiro,
  SituacaoTitulo,
  TipoMovimentacao,
  Titulo,
  TituloRequest,
  TransferenciaRequest,
  TransferenciaResponse,
  Usuario,
  UsuarioRequest,
} from "@/lib/types";

export function notifyError(error: unknown) {
  if (error instanceof ApiError) {
    toast.error(error.titulo, {
      description: [error.message, ...error.detalhes].join("\n"),
    });
    return;
  }
  toast.error("Erro inesperado", { description: "Tente novamente em instantes." });
}

function useFinanceMutation<TVariables, TResult>(
  fn: (variables: TVariables) => Promise<TResult>,
  sucesso?: string,
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: fn,
    onSuccess: async () => {
      await queryClient.invalidateQueries();
      if (sucesso) toast.success(sucesso);
    },
    onError: notifyError,
  });
}

export function useContas() {
  const usuarioId = useUsuarioId();
  return useQuery({
    queryKey: ["contas", usuarioId],
    queryFn: () => request<Conta[]>("/contas", { params: { usuarioId } }),
    enabled: usuarioId > 0,
  });
}

export function useConta(id: number) {
  return useQuery({
    queryKey: ["conta", id],
    queryFn: () => request<Conta>(`/contas/${id}`),
    enabled: id > 0,
  });
}

export function useExtrato(id: number) {
  return useQuery({
    queryKey: ["extrato", id],
    queryFn: () => request<Movimentacao[]>(`/contas/${id}/extrato`),
    enabled: id > 0,
  });
}

export function useSalvarConta() {
  return useFinanceMutation(
    ({ id, data }: { id?: number; data: ContaRequest }) =>
      id
        ? request<Conta>(`/contas/${id}`, { method: "PUT", body: data })
        : request<Conta>("/contas", { method: "POST", body: data }),
    "Conta salva",
  );
}

export function useRemoverConta() {
  return useFinanceMutation(
    (id: number) => request<void>(`/contas/${id}`, { method: "DELETE" }),
    "Conta removida",
  );
}

export function useCategorias(tipo?: TipoMovimentacao) {
  const usuarioId = useUsuarioId();
  return useQuery({
    queryKey: ["categorias", usuarioId, tipo ?? "TODOS"],
    queryFn: () => request<Categoria[]>("/categorias", { params: { usuarioId, tipo } }),
    enabled: usuarioId > 0,
  });
}

export function useSalvarCategoria() {
  return useFinanceMutation(
    ({ id, data }: { id?: number; data: CategoriaRequest }) =>
      id
        ? request<Categoria>(`/categorias/${id}`, { method: "PUT", body: data })
        : request<Categoria>("/categorias", { method: "POST", body: data }),
    "Categoria salva",
  );
}

export function useRemoverCategoria() {
  return useFinanceMutation(
    (id: number) => request<void>(`/categorias/${id}`, { method: "DELETE" }),
    "Categoria removida",
  );
}

export interface FiltroTitulos {
  tipo?: TipoMovimentacao;
  situacao?: SituacaoTitulo;
}

export function useTitulos(filtro: FiltroTitulos) {
  const usuarioId = useUsuarioId();
  return useQuery({
    queryKey: ["titulos", usuarioId, filtro.tipo ?? "TODOS", filtro.situacao ?? "TODAS"],
    queryFn: () =>
      request<Titulo[]>("/titulos", {
        params: { usuarioId, tipo: filtro.tipo, situacao: filtro.situacao },
      }),
    enabled: usuarioId > 0,
  });
}

export function useTitulo(id: number | null) {
  return useQuery({
    queryKey: ["titulo", id],
    queryFn: () => request<Titulo>(`/titulos/${id}`),
    enabled: !!id,
  });
}

export function useTitulosVencidos(referencia?: string) {
  const usuarioId = useUsuarioId();
  return useQuery({
    queryKey: ["titulos-vencidos", usuarioId, referencia ?? "hoje"],
    queryFn: () => request<Titulo[]>("/titulos/vencidos", { params: { usuarioId, referencia } }),
    enabled: usuarioId > 0,
  });
}

export function useSalvarTitulo() {
  return useFinanceMutation(
    ({ id, data }: { id?: number; data: TituloRequest }) =>
      id
        ? request<Titulo>(`/titulos/${id}`, { method: "PUT", body: data })
        : request<Titulo>("/titulos", { method: "POST", body: data }),
    "Título salvo",
  );
}

export function useLiquidarTitulo() {
  return useFinanceMutation(
    ({ id, tipo, data }: { id: number; tipo: TipoMovimentacao; data: LiquidacaoRequest }) =>
      request<Titulo>(`/titulos/${id}/${tipo === "DESPESA" ? "pagar" : "receber"}`, {
        method: "POST",
        body: data,
      }),
  );
}

export function useCancelarTitulo() {
  return useFinanceMutation(
    (id: number) => request<Titulo>(`/titulos/${id}/cancelar`, { method: "POST" }),
    "Título cancelado",
  );
}

export function useRemoverTitulo() {
  return useFinanceMutation(
    (id: number) => request<void>(`/titulos/${id}`, { method: "DELETE" }),
    "Título removido",
  );
}

export interface FiltroMovimentacoes {
  inicio?: string;
  fim?: string;
  tipo?: TipoMovimentacao;
}

export function useMovimentacoes(filtro: FiltroMovimentacoes = {}) {
  const usuarioId = useUsuarioId();
  return useQuery({
    queryKey: [
      "movimentacoes",
      usuarioId,
      filtro.inicio ?? "",
      filtro.fim ?? "",
      filtro.tipo ?? "TODOS",
    ],
    queryFn: () =>
      request<Movimentacao[]>("/movimentacoes", {
        params: { usuarioId, inicio: filtro.inicio, fim: filtro.fim, tipo: filtro.tipo },
      }),
    enabled: usuarioId > 0,
  });
}

export function useMovimentacao(id: number | null) {
  return useQuery({
    queryKey: ["movimentacao", id],
    queryFn: () => request<Movimentacao>(`/movimentacoes/${id}`),
    enabled: !!id,
  });
}

export function useRegistrarMovimentacao() {
  return useFinanceMutation(
    (data: MovimentacaoRequest) =>
      request<Movimentacao>("/movimentacoes", { method: "POST", body: data }),
    "Lançamento registrado",
  );
}

export function useTransferir() {
  return useFinanceMutation(
    (data: TransferenciaRequest) =>
      request<TransferenciaResponse>("/movimentacoes/transferir", { method: "POST", body: data }),
    "Transferência realizada",
  );
}

export function useEstornar() {
  return useFinanceMutation(
    (id: number) => request<void>(`/movimentacoes/${id}/estornar`, { method: "POST" }),
    "Movimentação estornada",
  );
}

export interface FiltroPosicao {
  dataReferencia?: string;
  janelaDias?: number;
}

export function usePosicao(tipo: TipoMovimentacao, filtro: FiltroPosicao) {
  const usuarioId = useUsuarioId();
  const rota = tipo === "DESPESA" ? "/contas-a-pagar" : "/contas-a-receber";
  return useQuery({
    queryKey: ["posicao", tipo, usuarioId, filtro.dataReferencia ?? "", filtro.janelaDias ?? ""],
    queryFn: () =>
      request<PosicaoTitulos>(rota, {
        params: { usuarioId, dataReferencia: filtro.dataReferencia, janelaDias: filtro.janelaDias },
      }),
    enabled: usuarioId > 0,
  });
}

export function useResumoFinanceiro(filtro: FiltroPosicao) {
  const usuarioId = useUsuarioId();
  return useQuery({
    queryKey: ["resumo", usuarioId, filtro.dataReferencia ?? "", filtro.janelaDias ?? ""],
    queryFn: () =>
      request<ResumoFinanceiro>("/resumo-financeiro", {
        params: { usuarioId, dataReferencia: filtro.dataReferencia, janelaDias: filtro.janelaDias },
      }),
    enabled: usuarioId > 0,
  });
}

export function useDre(inicio: string, fim: string, modo: ModoDre) {
  const usuarioId = useUsuarioId();
  return useQuery({
    queryKey: ["dre", usuarioId, inicio, fim, modo],
    queryFn: () => request<Dre>("/dre", { params: { usuarioId, inicio, fim, modo } }),
    enabled: usuarioId > 0 && !!inicio && !!fim,
    retry: false,
  });
}

export function useAtualizarUsuario() {
  return useFinanceMutation(
    ({ id, data }: { id: number; data: UsuarioRequest }) =>
      request<Usuario>(`/usuarios/${id}`, { method: "PUT", body: data }),
    "Perfil atualizado",
  );
}

export function useRemoverUsuario() {
  return useMutation({
    mutationFn: (id: number) => request<void>(`/usuarios/${id}`, { method: "DELETE" }),
    onError: notifyError,
  });
}

export function useCadastrarUsuario() {
  return useMutation({
    mutationFn: (data: UsuarioRequest) =>
      request<Usuario>("/usuarios", { method: "POST", body: data }),
    onError: notifyError,
  });
}
