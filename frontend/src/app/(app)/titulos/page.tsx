"use client";

import { useState } from "react";
import { Ban, CalendarClock, Eye, HandCoins, MoreHorizontal, Pencil, Plus, Trash } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Skeleton } from "skullet/react";
import { LinhasCarregando } from "@/components/linhas-carregando";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { ConfirmDialog } from "@/components/confirm-dialog";
import { DatePicker } from "@/components/date-picker";
import { EmptyState } from "@/components/empty-state";
import { ErrorState } from "@/components/error-state";
import { LiquidarDialog, type TituloParaLiquidar } from "@/components/forms/liquidar-dialog";
import { TituloDialog } from "@/components/forms/titulo-dialog";
import { Money } from "@/components/money";
import { PageHeader } from "@/components/page-header";
import { SituacaoBadge } from "@/components/status-badge";
import { TituloSheet } from "@/components/titulo-sheet";
import { formatDate } from "@/lib/format";
import { useCancelarTitulo, useRemoverTitulo, useTitulos, useTitulosVencidos } from "@/lib/queries";
import type { SituacaoTitulo, TipoMovimentacao, Titulo } from "@/lib/types";
import { cn } from "@/lib/utils";

type Visao = "todos" | "vencidos";
type FiltroTipo = "TODOS" | TipoMovimentacao;
type FiltroSituacao = "TODAS" | SituacaoTitulo;

export default function TitulosPage() {
  const [visao, setVisao] = useState<Visao>("todos");
  const [tipo, setTipo] = useState<FiltroTipo>("TODOS");
  const [situacao, setSituacao] = useState<FiltroSituacao>("TODAS");
  const [referencia, setReferencia] = useState<string | undefined>();

  const listagem = useTitulos({
    tipo: tipo === "TODOS" ? undefined : tipo,
    situacao: situacao === "TODAS" ? undefined : situacao,
  });
  const vencidos = useTitulosVencidos(referencia);
  const consulta = visao === "todos" ? listagem : vencidos;
  const titulos = consulta.data ?? [];

  const [dialogAberto, setDialogAberto] = useState(false);
  const [editando, setEditando] = useState<Titulo | null>(null);
  const [detalhe, setDetalhe] = useState<number | null>(null);
  const [liquidando, setLiquidando] = useState<TituloParaLiquidar | null>(null);
  const [cancelando, setCancelando] = useState<Titulo | null>(null);
  const [excluindo, setExcluindo] = useState<Titulo | null>(null);
  const cancelar = useCancelarTitulo();
  const remover = useRemoverTitulo();

  const abrirNovo = () => {
    setEditando(null);
    setDialogAberto(true);
  };

  return (
    <div className="grid gap-8">
      <PageHeader
        title="Títulos"
        description="Cadastrar um título não altera o saldo nem o resultado realizado."
        actions={
          <Button onClick={abrirNovo}>
            <Plus />
            Novo título
          </Button>
        }
      />

      <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
        <Tabs value={visao} onValueChange={(v) => setVisao(v as Visao)}>
          <TabsList>
            <TabsTrigger value="todos" className="px-4">
              Todos
            </TabsTrigger>
            <TabsTrigger value="vencidos" className="px-4">
              Vencidos
            </TabsTrigger>
          </TabsList>
        </Tabs>
        <div className="flex flex-wrap items-center gap-2">
          {visao === "todos" ? (
            <>
              <Tabs value={tipo} onValueChange={(v) => setTipo(v as FiltroTipo)}>
                <TabsList>
                  <TabsTrigger value="TODOS" className="px-3">
                    Todos
                  </TabsTrigger>
                  <TabsTrigger value="DESPESA" className="px-3">
                    A pagar
                  </TabsTrigger>
                  <TabsTrigger value="RECEITA" className="px-3">
                    A receber
                  </TabsTrigger>
                </TabsList>
              </Tabs>
              <Select value={situacao} onValueChange={(v) => setSituacao(v as FiltroSituacao)}>
                <SelectTrigger className="w-44">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent position="popper">
                  <SelectItem value="TODAS">Todas as situações</SelectItem>
                  <SelectItem value="PENDENTE">Pendente</SelectItem>
                  <SelectItem value="PAGO">Quitado</SelectItem>
                  <SelectItem value="CANCELADO">Cancelado</SelectItem>
                </SelectContent>
              </Select>
            </>
          ) : (
            <div className="w-56">
              <DatePicker value={referencia} onChange={setReferencia} placeholder="Referência: hoje" clearable />
            </div>
          )}
        </div>
      </div>

      <div className="overflow-hidden rounded-2xl border bg-card">
        {consulta.error ? (
          <ErrorState error={consulta.error} onRetry={consulta.refetch} />
        ) : !consulta.isLoading && titulos.length === 0 ? (
          <EmptyState
            icon={CalendarClock}
            title={visao === "vencidos" ? "Nenhum título vencido" : "Nenhum título encontrado"}
            description={
              visao === "vencidos"
                ? "Não há títulos pendentes com vencimento ultrapassado na data de referência."
                : "Cadastre contas a pagar e a receber para acompanhar o que está previsto."
            }
            action={
              visao === "todos" && (
                <Button onClick={abrirNovo}>
                  <Plus />
                  Novo título
                </Button>
              )
            }
          />
        ) : (
          <Skeleton id="titulos-1" loading={consulta.isLoading}>
            <Table>
              <TableHeader>
                <TableRow className="hover:bg-transparent">
                  <TableHead className="pl-5">Título</TableHead>
                  <TableHead>Vencimento</TableHead>
                  <TableHead className="hidden text-right md:table-cell">Previsto</TableHead>
                  <TableHead className="hidden w-[180px] lg:table-cell">Realizado</TableHead>
                  <TableHead className="text-right">Em aberto</TableHead>
                  <TableHead>Situação</TableHead>
                  <TableHead className="w-12 pr-5" />
                </TableRow>
              </TableHeader>
              <TableBody>
                {consulta.isLoading && titulos.length === 0 && (
                  <LinhasCarregando celulas={["pl-5", "", "hidden md:table-cell", "hidden lg:table-cell", "", "", "pr-5"]} />
                )}
                {titulos.map((titulo) => {
                  const percentual = Math.min(100, (titulo.valorRealizado / titulo.valorPrevisto) * 100);
                  const podeAlterar = titulo.situacao === "PENDENTE";
                  const semBaixas = titulo.quantidadeMovimentacoes === 0;
                  return (
                    <TableRow key={titulo.idTitulo} className="cursor-pointer" onClick={() => setDetalhe(titulo.idTitulo)}>
                      <TableCell className="pl-5">
                        <p className="max-w-[280px] truncate font-medium" title={titulo.descricao}>
                          {titulo.descricao}
                        </p>
                        <p className="text-xs text-muted-foreground">
                          {titulo.tipo === "RECEITA" ? "A receber" : "A pagar"} · {titulo.categoriaNome}
                        </p>
                      </TableCell>
                      <TableCell className="text-muted-foreground tabular">{formatDate(titulo.dataVencimento, "dd/MM/yyyy")}</TableCell>
                      <TableCell className="hidden text-right md:table-cell">
                        <Money value={titulo.valorPrevisto} />
                      </TableCell>
                      <TableCell className="hidden lg:table-cell">
                        <div className="flex items-center gap-2">
                          <div className="h-1.5 flex-1 overflow-hidden rounded-full bg-secondary">
                            <div
                              className={cn("h-full rounded-full", titulo.tipo === "RECEITA" ? "bg-receita" : "bg-despesa")}
                              style={{ width: `${percentual}%` }}
                            />
                          </div>
                          <span className="w-9 text-right text-xs text-muted-foreground tabular">{Math.round(percentual)}%</span>
                        </div>
                      </TableCell>
                      <TableCell className="text-right font-medium">
                        <Money value={titulo.valorEmAberto} />
                      </TableCell>
                      <TableCell>
                        <SituacaoBadge situacao={titulo.situacaoEfetiva} />
                      </TableCell>
                      <TableCell className="pr-5 text-right" onClick={(e) => e.stopPropagation()}>
                        <DropdownMenu>
                          <DropdownMenuTrigger asChild>
                            <Button variant="ghost" size="icon-sm" aria-label="Ações do título">
                              <MoreHorizontal />
                            </Button>
                          </DropdownMenuTrigger>
                          <DropdownMenuContent align="end" className="w-48">
                            <DropdownMenuItem onSelect={() => setDetalhe(titulo.idTitulo)}>
                              <Eye />
                              Detalhes
                            </DropdownMenuItem>
                            {titulo.podeRegistrarPagamento && (
                              <DropdownMenuItem onSelect={() => setLiquidando(titulo)}>
                                <HandCoins />
                                {titulo.tipo === "DESPESA" ? "Pagar" : "Receber"}
                              </DropdownMenuItem>
                            )}
                            {podeAlterar && (
                              <DropdownMenuItem
                                onSelect={() => {
                                  setEditando(titulo);
                                  setDialogAberto(true);
                                }}
                              >
                                <Pencil />
                                Editar
                              </DropdownMenuItem>
                            )}
                            {semBaixas && <DropdownMenuSeparator />}
                            {podeAlterar && semBaixas && (
                              <DropdownMenuItem onSelect={() => setCancelando(titulo)}>
                                <Ban />
                                Cancelar título
                              </DropdownMenuItem>
                            )}
                            {semBaixas && (
                              <DropdownMenuItem variant="destructive" onSelect={() => setExcluindo(titulo)}>
                                <Trash />
                                Excluir
                              </DropdownMenuItem>
                            )}
                          </DropdownMenuContent>
                        </DropdownMenu>
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          </Skeleton>
        )}
      </div>

      <TituloDialog
        open={dialogAberto}
        onOpenChange={setDialogAberto}
        titulo={editando}
        tipoInicial={tipo === "TODOS" ? undefined : tipo}
      />
      <TituloSheet id={detalhe} onOpenChange={(open) => !open && setDetalhe(null)} onLiquidar={(titulo) => setLiquidando(titulo)} />
      <LiquidarDialog titulo={liquidando} onOpenChange={(open) => !open && setLiquidando(null)} />
      <ConfirmDialog
        open={!!cancelando}
        onOpenChange={(open) => !open && setCancelando(null)}
        title="Cancelar título?"
        description={
          <>
            O título <strong className="text-foreground">{cancelando?.descricao}</strong> sairá do resultado previsto. O cancelamento só é
            permitido quando nada foi realizado.
          </>
        }
        confirmLabel="Cancelar título"
        destructive
        loading={cancelar.isPending}
        onConfirm={() => cancelando && cancelar.mutate(cancelando.idTitulo, { onSuccess: () => setCancelando(null) })}
      />
      <ConfirmDialog
        open={!!excluindo}
        onOpenChange={(open) => !open && setExcluindo(null)}
        title="Excluir título?"
        description={
          <>
            O título <strong className="text-foreground">{excluindo?.descricao}</strong> será removido definitivamente. Títulos com
            movimentações vinculadas não podem ser excluídos.
          </>
        }
        confirmLabel="Excluir"
        destructive
        loading={remover.isPending}
        onConfirm={() => excluindo && remover.mutate(excluindo.idTitulo, { onSuccess: () => setExcluindo(null) })}
      />
    </div>
  );
}
