"use client";

import { useState } from "react";
import Link from "next/link";
import { ArrowLeftRight, MoreHorizontal, Pencil, Plus, ReceiptText, Trash, Wallet } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Skeleton } from "skullet/react";
import { ConfirmDialog } from "@/components/confirm-dialog";
import { EmptyState } from "@/components/empty-state";
import { ErrorState } from "@/components/error-state";
import { ContaDialog } from "@/components/forms/conta-dialog";
import { TransferenciaDialog } from "@/components/forms/transferencia-dialog";
import { Money } from "@/components/money";
import { PageHeader } from "@/components/page-header";
import { formatMoney, labelTipoConta } from "@/lib/format";
import { useContas, useRemoverConta } from "@/lib/queries";
import type { Conta } from "@/lib/types";
import { cn } from "@/lib/utils";

export default function ContasPage() {
  const { data: contas = [], isLoading, error, refetch } = useContas();
  const remover = useRemoverConta();
  const [dialogAberto, setDialogAberto] = useState(false);
  const [editando, setEditando] = useState<Conta | null>(null);
  const [excluindo, setExcluindo] = useState<Conta | null>(null);
  const [transferindo, setTransferindo] = useState(false);

  const saldoTotal = contas.reduce((total, conta) => total + conta.saldoAtual, 0);

  return (
    <div className="grid gap-8">
      <PageHeader
        title="Contas"
        actions={
          <>
            <Button variant="outline" onClick={() => setTransferindo(true)} disabled={contas.length < 2}>
              <ArrowLeftRight />
              Transferir
            </Button>
            <Button
              onClick={() => {
                setEditando(null);
                setDialogAberto(true);
              }}
            >
              <Plus />
              Nova conta
            </Button>
          </>
        }
      />

      {error ? (
        <ErrorState error={error} onRetry={refetch} />
      ) : (
        <>
          <div className="@container rounded-2xl border bg-card p-6 sm:p-7">
            <Skeleton id="contas-1" loading={isLoading} label="Carregando saldo das contas">
              <div className="flex flex-col gap-6 xl:flex-row xl:items-end xl:justify-between">
                <div className="min-w-0">
                  <p className="text-sm text-muted-foreground">Saldo somado das contas</p>
                  <p
                    className={cn(
                      "mt-2 text-[clamp(1.5rem,8cqi,2.5rem)] font-semibold tracking-tight whitespace-nowrap tabular",
                      saldoTotal < 0 && "text-despesa",
                    )}
                  >
                    {formatMoney(saldoTotal)}
                  </p>
                </div>
                <div className="flex flex-wrap gap-x-8 gap-y-4 text-sm">
                  <div>
                    <p className="text-muted-foreground">Contas</p>
                    <p className="mt-1 text-xl font-semibold tabular">{contas.length}</p>
                  </div>
                  <div>
                    <p className="text-muted-foreground">Saldo inicial somado</p>
                    <p className="mt-1 text-xl font-semibold tabular">{formatMoney(contas.reduce((t, c) => t + c.saldoInicial, 0))}</p>
                  </div>
                </div>
              </div>
            </Skeleton>
          </div>

          <Skeleton id="contas-2" loading={isLoading}>
            {contas.length === 0 ? (
              <div className="rounded-2xl border bg-card">
                <EmptyState
                  icon={Wallet}
                  title="Nenhuma conta cadastrada"
                  description="Cadastre suas contas bancárias, carteiras e investimentos para registrar movimentações."
                  action={
                    <Button onClick={() => setDialogAberto(true)}>
                      <Plus />
                      Nova conta
                    </Button>
                  }
                />
              </div>
            ) : (
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-3">
                {contas.map((conta) => {
                  const variacao = conta.saldoAtual - conta.saldoInicial;
                  return (
                    <div
                      key={conta.idConta}
                      className="@container relative flex min-w-0 flex-col rounded-2xl border bg-card p-5 transition-shadow hover:shadow-lg hover:shadow-foreground/5"
                    >
                      <div className="flex items-start justify-between gap-3">
                        <div className="min-w-0">
                          <p className="font-medium [overflow-wrap:anywhere]">{conta.nome}</p>
                          <p className="text-xs text-muted-foreground [overflow-wrap:anywhere]">{labelTipoConta(conta.tipo)}</p>
                        </div>
                        <DropdownMenu>
                          <DropdownMenuTrigger asChild>
                            <Button variant="ghost" size="icon-sm" aria-label="Ações da conta">
                              <MoreHorizontal />
                            </Button>
                          </DropdownMenuTrigger>
                          <DropdownMenuContent align="end" className="w-44">
                            <DropdownMenuItem asChild>
                              <Link href={`/contas/${conta.idConta}`}>
                                <ReceiptText />
                                Ver extrato
                              </Link>
                            </DropdownMenuItem>
                            <DropdownMenuItem
                              onSelect={() => {
                                setEditando(conta);
                                setDialogAberto(true);
                              }}
                            >
                              <Pencil />
                              Editar
                            </DropdownMenuItem>
                            <DropdownMenuSeparator />
                            <DropdownMenuItem variant="destructive" onSelect={() => setExcluindo(conta)}>
                              <Trash />
                              Excluir
                            </DropdownMenuItem>
                          </DropdownMenuContent>
                        </DropdownMenu>
                      </div>

                      <div className="mt-8">
                        <p className="text-xs text-muted-foreground">Saldo atual</p>
                        <p
                          className={cn(
                            "mt-1 text-[clamp(1.25rem,9cqi,1.75rem)] font-semibold tracking-tight whitespace-nowrap",
                            conta.saldoAtual < 0 && "text-despesa",
                          )}
                        >
                          <Money value={conta.saldoAtual} />
                        </p>
                      </div>

                      <div className="mt-4 flex flex-wrap items-center justify-between gap-x-3 gap-y-1 border-t pt-4 text-xs">
                        <span className="text-muted-foreground">
                          Inicial <span className="tabular text-foreground">{formatMoney(conta.saldoInicial)}</span>
                        </span>
                        <span
                          className={cn(
                            "tabular font-medium",
                            variacao > 0 ? "text-receita" : variacao < 0 ? "text-despesa" : "text-muted-foreground",
                          )}
                        >
                          {variacao > 0 ? "+" : variacao < 0 ? "−" : ""}
                          {formatMoney(Math.abs(variacao))}
                        </span>
                      </div>

                      <Link href={`/contas/${conta.idConta}`} className="mt-4 text-sm font-medium text-foreground/80 hover:text-foreground">
                        Ver extrato
                      </Link>
                    </div>
                  );
                })}
              </div>
            )}
          </Skeleton>
        </>
      )}

      <ContaDialog open={dialogAberto} onOpenChange={setDialogAberto} conta={editando} />
      <TransferenciaDialog open={transferindo} onOpenChange={setTransferindo} />
      <ConfirmDialog
        open={!!excluindo}
        onOpenChange={(open) => !open && setExcluindo(null)}
        title="Excluir conta?"
        description={
          <>
            A conta <strong className="text-foreground">{excluindo?.nome}</strong> será removida. Contas que já possuem movimentações não
            podem ser excluídas, para preservar o histórico do caixa.
          </>
        }
        confirmLabel="Excluir"
        destructive
        loading={remover.isPending}
        onConfirm={() => excluindo && remover.mutate(excluindo.idConta, { onSuccess: () => setExcluindo(null) })}
      />
    </div>
  );
}
