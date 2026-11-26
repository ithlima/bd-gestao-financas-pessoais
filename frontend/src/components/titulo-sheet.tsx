"use client";

import { useState } from "react";
import { HandCoins, Undo2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from "@/components/ui/sheet";
import { Skeleton } from "skullet/react";
import { ConfirmDialog } from "@/components/confirm-dialog";
import { Money } from "@/components/money";
import { DetalheLinha } from "@/components/movimentacoes";
import { SituacaoBadge, TipoBadge } from "@/components/status-badge";
import { formatDate } from "@/lib/format";
import { useEstornar, useMovimentacoes, useTitulo } from "@/lib/queries";
import type { Movimentacao, Titulo } from "@/lib/types";
import { cn } from "@/lib/utils";

interface TituloSheetProps {
  id: number | null;
  onOpenChange: (open: boolean) => void;
  onLiquidar: (titulo: Titulo) => void;
}

export function TituloSheet({ id, onOpenChange, onLiquidar }: TituloSheetProps) {
  const { data: titulo } = useTitulo(id);
  const movimentacoes = useMovimentacoes();
  const estornar = useEstornar();
  const [estornando, setEstornando] = useState<Movimentacao | null>(null);

  const baixas = (movimentacoes.data ?? []).filter((m) => m.tituloId === id);
  const percentual = titulo ? Math.min(100, (titulo.valorRealizado / titulo.valorPrevisto) * 100) : 0;

  return (
    <Sheet open={!!id} onOpenChange={onOpenChange}>
      <SheetContent className="w-full gap-0 sm:max-w-md">
        <SheetHeader className="border-b p-6">
          <SheetTitle>Título #{id}</SheetTitle>
          <SheetDescription>Compromisso previsto e suas baixas realizadas.</SheetDescription>
        </SheetHeader>
        <Skeleton id="titulo-detalhe-1" data={titulo} label="Carregando título">
          {(titulo) => (
            <div className="flex-1 overflow-y-auto p-6">
              <div className="rounded-2xl border bg-secondary/40 p-5">
                <div className="flex flex-wrap items-center gap-2">
                  <TipoBadge tipo={titulo.tipo} />
                  <SituacaoBadge situacao={titulo.situacaoEfetiva} />
                </div>
                <p className="mt-3 text-lg font-semibold tracking-tight">{titulo.descricao}</p>
                <p className="text-sm text-muted-foreground">{titulo.categoriaNome}</p>
                <div className="mt-5 flex items-end justify-between">
                  <div>
                    <p className="text-xs text-muted-foreground">Realizado</p>
                    <p className="text-xl font-semibold">
                      <Money value={titulo.valorRealizado} />
                    </p>
                  </div>
                  <div className="text-right">
                    <p className="text-xs text-muted-foreground">Previsto</p>
                    <p className="text-sm font-medium">
                      <Money value={titulo.valorPrevisto} />
                    </p>
                  </div>
                </div>
                <div className="mt-3 h-2 overflow-hidden rounded-full bg-border">
                  <div
                    className={cn("h-full rounded-full", titulo.tipo === "RECEITA" ? "bg-receita" : "bg-despesa")}
                    style={{ width: `${percentual}%` }}
                  />
                </div>
              </div>

              <dl className="mt-6 divide-y rounded-xl border">
                <DetalheLinha termo="Vencimento" valor={formatDate(titulo.dataVencimento)} />
                <DetalheLinha termo="Quitação" valor={formatDate(titulo.dataPagamento)} />
                <DetalheLinha termo="Em aberto" valor={<Money value={titulo.valorEmAberto} />} />
                <DetalheLinha termo="Baixas registradas" valor={titulo.quantidadeMovimentacoes} />
                {titulo.observacao && <DetalheLinha termo="Observação" valor={titulo.observacao} />}
              </dl>

              {titulo.podeRegistrarPagamento && (
                <Button className="mt-6 w-full" onClick={() => onLiquidar(titulo)}>
                  <HandCoins />
                  {titulo.tipo === "DESPESA" ? "Registrar pagamento" : "Registrar recebimento"}
                </Button>
              )}

              <div className="mt-8">
                <p className="mb-3 text-sm font-medium">Baixas</p>
                <Skeleton id="titulo-detalhe-2" loading={movimentacoes.isLoading} label="Carregando baixas">
                  {baixas.length === 0 ? (
                    <p className="rounded-xl border border-dashed px-4 py-6 text-center text-sm text-muted-foreground">
                      Nenhuma baixa registrada para este título.
                    </p>
                  ) : (
                    <ul className="divide-y rounded-xl border">
                      {baixas.map((mov) => (
                        <li key={mov.idMovimentacao} className="flex items-center justify-between gap-3 px-4 py-3">
                          <div className="min-w-0">
                            <p className="text-sm font-medium">
                              <Money value={mov.valor} tipo={mov.tipo} colored />
                            </p>
                            <p className="truncate text-xs text-muted-foreground">
                              {formatDate(mov.data)} · {mov.contaNome}
                            </p>
                          </div>
                          <Button variant="ghost" size="sm" onClick={() => setEstornando(mov)}>
                            <Undo2 />
                            Estornar
                          </Button>
                        </li>
                      ))}
                    </ul>
                  )}
                </Skeleton>
              </div>
            </div>
          )}
        </Skeleton>
      </SheetContent>

      <ConfirmDialog
        open={!!estornando}
        onOpenChange={(open) => !open && setEstornando(null)}
        title="Estornar baixa?"
        description="A movimentação será removida. Se o título estava quitado e o realizado ficar abaixo do previsto, ele volta para pendente."
        confirmLabel="Estornar"
        destructive
        loading={estornar.isPending}
        onConfirm={() => estornando && estornar.mutate(estornando.idMovimentacao, { onSuccess: () => setEstornando(null) })}
      />
    </Sheet>
  );
}
