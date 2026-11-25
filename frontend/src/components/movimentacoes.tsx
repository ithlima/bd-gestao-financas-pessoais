"use client";

import { useState } from "react";
import { Eye, MoreHorizontal, Undo2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from "@/components/ui/dropdown-menu";
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from "@/components/ui/sheet";
import { Skeleton } from "skullet/react";
import { LinhasCarregando } from "@/components/linhas-carregando";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { ConfirmDialog } from "@/components/confirm-dialog";
import { Money } from "@/components/money";
import { Pill } from "@/components/status-badge";
import { formatDate, formatMoney } from "@/lib/format";
import { useEstornar, useMovimentacao } from "@/lib/queries";
import type { Movimentacao } from "@/lib/types";
import { cn } from "@/lib/utils";

export type OrigemMovimentacao = "titulo" | "transferencia" | "avulsa";

export function origemDe(mov: Pick<Movimentacao, "tituloId" | "categoriaId">): OrigemMovimentacao {
  if (mov.tituloId) return "titulo";
  if (!mov.categoriaId) return "transferencia";
  return "avulsa";
}

export function OrigemPill({ mov }: { mov: Pick<Movimentacao, "tituloId" | "categoriaId"> }) {
  const origem = origemDe(mov);
  if (origem === "titulo")
    return (
      <Pill tone="neutral" dot={false}>
        Título #{mov.tituloId}
      </Pill>
    );
  if (origem === "transferencia")
    return (
      <Pill tone="ink" dot={false}>
        Transferência
      </Pill>
    );
  return (
    <Pill tone="neutral" dot={false}>
      Avulsa
    </Pill>
  );
}

function textoEstorno(mov: Movimentacao) {
  const origem = origemDe(mov);
  if (origem === "transferencia")
    return "Esta movimentação faz parte de uma transferência. As duas pontas (saída na origem e entrada no destino) serão estornadas juntas.";
  if (origem === "titulo")
    return `A movimentação será removida. Se o título #${mov.tituloId} estava quitado e o realizado ficar abaixo do previsto, ele volta para pendente.`;
  return "A movimentação será removida e o saldo da conta recalculado.";
}

interface MovimentacoesTableProps {
  movimentacoes: Movimentacao[];
  loading?: boolean;
  mostrarConta?: boolean;
  saldoInicial?: number;
}

export function MovimentacoesTable({ movimentacoes, loading, mostrarConta = true, saldoInicial }: MovimentacoesTableProps) {
  const [detalhe, setDetalhe] = useState<number | null>(null);
  const [estornando, setEstornando] = useState<Movimentacao | null>(null);
  const estornar = useEstornar();

  const saldos: number[] = [];
  if (saldoInicial !== undefined) {
    let acumulado = saldoInicial;
    for (const mov of movimentacoes) {
      acumulado += mov.tipo === "RECEITA" ? mov.valor : -mov.valor;
      saldos.push(acumulado);
    }
  }

  return (
    <>
      <Skeleton id={saldoInicial !== undefined ? "movimentacoes-extrato" : "movimentacoes-geral"} loading={loading}>
        <Table>
          <TableHeader>
            <TableRow className="hover:bg-transparent">
              <TableHead className="w-[120px] pl-5">Data</TableHead>
              <TableHead>Descrição</TableHead>
              {mostrarConta && <TableHead className="hidden md:table-cell">Conta</TableHead>}
              <TableHead className="hidden lg:table-cell">Categoria</TableHead>
              <TableHead className="text-right">Valor</TableHead>
              {saldoInicial !== undefined && <TableHead className="hidden text-right sm:table-cell">Saldo</TableHead>}
              <TableHead className="w-12 pr-5" />
            </TableRow>
          </TableHeader>
          <TableBody>
            {loading && movimentacoes.length === 0 && (
              <LinhasCarregando
                celulas={[
                  "pl-5",
                  "",
                  mostrarConta && "hidden md:table-cell",
                  "hidden lg:table-cell",
                  "",
                  saldoInicial !== undefined && "hidden sm:table-cell",
                  "pr-5",
                ]}
              />
            )}
            {movimentacoes.map((mov, index) => (
              <TableRow key={mov.idMovimentacao} className="group">
                <TableCell className="pl-5 text-muted-foreground tabular">{formatDate(mov.data, "dd/MM/yyyy")}</TableCell>
                <TableCell>
                  <p className="max-w-[260px] truncate font-medium" title={mov.descricao}>
                    {mov.descricao}
                  </p>
                  <div className="mt-1">
                    <OrigemPill mov={mov} />
                  </div>
                </TableCell>
                {mostrarConta && <TableCell className="hidden text-muted-foreground md:table-cell">{mov.contaNome}</TableCell>}
                <TableCell className="hidden text-muted-foreground lg:table-cell">{mov.categoriaNome ?? "—"}</TableCell>
                <TableCell className="text-right font-medium">
                  <Money value={mov.valor} tipo={mov.tipo} colored />
                </TableCell>
                {saldoInicial !== undefined && (
                  <TableCell className="hidden text-right text-muted-foreground sm:table-cell">
                    <Money value={saldos[index]} />
                  </TableCell>
                )}
                <TableCell className="pr-5 text-right">
                  <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                      <Button variant="ghost" size="icon-sm" aria-label="Ações">
                        <MoreHorizontal />
                      </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="end" className="w-44">
                      <DropdownMenuItem onSelect={() => setDetalhe(mov.idMovimentacao)}>
                        <Eye />
                        Detalhes
                      </DropdownMenuItem>
                      <DropdownMenuItem variant="destructive" onSelect={() => setEstornando(mov)}>
                        <Undo2 />
                        Estornar
                      </DropdownMenuItem>
                    </DropdownMenuContent>
                  </DropdownMenu>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </Skeleton>

      <MovimentacaoSheet
        id={detalhe}
        onOpenChange={(open) => !open && setDetalhe(null)}
        onEstornar={(mov) => {
          setDetalhe(null);
          setEstornando(mov);
        }}
      />

      <ConfirmDialog
        open={!!estornando}
        onOpenChange={(open) => !open && setEstornando(null)}
        title="Estornar movimentação?"
        description={estornando ? textoEstorno(estornando) : ""}
        confirmLabel="Estornar"
        destructive
        loading={estornar.isPending}
        onConfirm={() => estornando && estornar.mutate(estornando.idMovimentacao, { onSuccess: () => setEstornando(null) })}
      />
    </>
  );
}

function MovimentacaoSheet({
  id,
  onOpenChange,
  onEstornar,
}: {
  id: number | null;
  onOpenChange: (open: boolean) => void;
  onEstornar: (mov: Movimentacao) => void;
}) {
  const { data: mov } = useMovimentacao(id);

  return (
    <Sheet open={!!id} onOpenChange={onOpenChange}>
      <SheetContent className="w-full gap-0 sm:max-w-md">
        <SheetHeader className="border-b p-6">
          <SheetTitle>Movimentação #{id}</SheetTitle>
          <SheetDescription>Fato realizado no caixa. Correções são feitas por estorno.</SheetDescription>
        </SheetHeader>
        <Skeleton id="movimentacao-detalhe" data={mov} label="Carregando movimentação">
          {(mov) => (
            <div className="flex-1 overflow-y-auto p-6">
              <div
                className={cn(
                  "rounded-2xl p-5",
                  origemDe(mov) === "transferencia" ? "bg-secondary" : mov.tipo === "RECEITA" ? "bg-receita-soft" : "bg-despesa-soft",
                )}
              >
                <p className="text-xs font-medium text-muted-foreground">{mov.tipo === "RECEITA" ? "Entrada" : "Saída"}</p>
                <p className="mt-1 text-3xl font-semibold tracking-tight">
                  <Money value={mov.valor} tipo={mov.tipo} colored={origemDe(mov) !== "transferencia"} />
                </p>
                <p className="mt-2 font-medium">{mov.descricao}</p>
              </div>
              <dl className="mt-6 grid gap-0 divide-y rounded-xl border">
                <DetalheLinha termo="Data" valor={formatDate(mov.data, "dd 'de' MMMM 'de' yyyy")} />
                <DetalheLinha termo="Conta" valor={mov.contaNome} />
                <DetalheLinha termo="Categoria" valor={mov.categoriaNome ?? "Sem categoria"} />
                <DetalheLinha termo="Origem" valor={<OrigemPill mov={mov} />} />
                <DetalheLinha termo="Valor" valor={formatMoney(mov.valor)} />
              </dl>
              <Button variant="destructive" className="mt-6 w-full" onClick={() => onEstornar(mov)}>
                <Undo2 />
                Estornar movimentação
              </Button>
            </div>
          )}
        </Skeleton>
      </SheetContent>
    </Sheet>
  );
}

export function DetalheLinha({ termo, valor }: { termo: string; valor: React.ReactNode }) {
  return (
    <div className="flex items-start justify-between gap-4 px-4 py-3 text-sm">
      <dt className="shrink-0 text-muted-foreground">{termo}</dt>
      <dd className="min-w-0 text-right font-medium whitespace-pre-line [overflow-wrap:anywhere]">{valor}</dd>
    </div>
  );
}
