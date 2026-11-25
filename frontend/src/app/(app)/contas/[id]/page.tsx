"use client";

import { useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { ArrowLeft, ArrowLeftRight, Plus, ReceiptText } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Skeleton } from "skullet/react";
import { EmptyState } from "@/components/empty-state";
import { ErrorState } from "@/components/error-state";
import { MovimentacaoDialog } from "@/components/forms/movimentacao-dialog";
import { TransferenciaDialog } from "@/components/forms/transferencia-dialog";
import { Money } from "@/components/money";
import { MovimentacoesTable } from "@/components/movimentacoes";
import { StatCard } from "@/components/stat-card";
import { labelTipoConta, plural } from "@/lib/format";
import { useConta, useExtrato } from "@/lib/queries";

export default function ExtratoPage() {
  const params = useParams<{ id: string }>();
  const id = Number(params.id);
  const conta = useConta(id);
  const extrato = useExtrato(id);
  const [lancando, setLancando] = useState(false);
  const [transferindo, setTransferindo] = useState(false);

  const movimentacoes = extrato.data ?? [];
  const entradas = movimentacoes.filter((m) => m.tipo === "RECEITA").reduce((t, m) => t + m.valor, 0);
  const saidas = movimentacoes.filter((m) => m.tipo === "DESPESA").reduce((t, m) => t + m.valor, 0);

  if (conta.error) {
    return <ErrorState error={conta.error} onRetry={conta.refetch} />;
  }

  return (
    <div className="grid gap-8">
      <div>
        <Button variant="ghost" size="sm" asChild className="-ml-2 mb-4 text-muted-foreground">
          <Link href="/contas">
            <ArrowLeft />
            Contas
          </Link>
        </Button>
        <div className="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
          <div className="min-w-0">
            <Skeleton id="extrato-1" loading={conta.isLoading} label="Carregando conta">
              <h1 className="text-2xl font-semibold tracking-tight [overflow-wrap:anywhere] md:text-[28px]">{conta.data?.nome}</h1>
              <p className="mt-1.5 text-sm text-muted-foreground [overflow-wrap:anywhere]">
                {conta.data ? labelTipoConta(conta.data.tipo) : ""}
              </p>
            </Skeleton>
          </div>
          <div className="flex flex-wrap gap-2">
            <Button variant="outline" onClick={() => setTransferindo(true)}>
              <ArrowLeftRight />
              Transferir
            </Button>
            <Button onClick={() => setLancando(true)}>
              <Plus />
              Lançamento avulso
            </Button>
          </div>
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Saldo atual" loading={conta.isLoading} value={<Money value={conta.data?.saldoAtual} />} />
        <StatCard label="Saldo inicial" loading={conta.isLoading} value={<Money value={conta.data?.saldoInicial} />} />
        <StatCard
          label="Entradas"
          loading={extrato.isLoading}
          value={<Money value={entradas} />}
          hint={plural(movimentacoes.filter((m) => m.tipo === "RECEITA").length, "lançamento", "lançamentos")}
        />
        <StatCard
          label="Saídas"
          loading={extrato.isLoading}
          value={<Money value={saidas} />}
          hint={plural(movimentacoes.filter((m) => m.tipo === "DESPESA").length, "lançamento", "lançamentos")}
        />
      </div>

      <div className="overflow-hidden rounded-2xl border bg-card">
        {extrato.error ? (
          <ErrorState error={extrato.error} onRetry={extrato.refetch} />
        ) : !extrato.isLoading && movimentacoes.length === 0 ? (
          <EmptyState
            icon={ReceiptText}
            title="Nenhuma movimentação nesta conta"
            description="Pagamentos, recebimentos, lançamentos avulsos e transferências aparecerão aqui."
          />
        ) : (
          <MovimentacoesTable
            movimentacoes={movimentacoes}
            loading={extrato.isLoading || conta.isLoading}
            mostrarConta={false}
            saldoInicial={conta.data?.saldoInicial ?? 0}
          />
        )}
      </div>

      <MovimentacaoDialog open={lancando} onOpenChange={setLancando} contaInicial={id} />
      <TransferenciaDialog open={transferindo} onOpenChange={setTransferindo} contaOrigemInicial={id} />
    </div>
  );
}
