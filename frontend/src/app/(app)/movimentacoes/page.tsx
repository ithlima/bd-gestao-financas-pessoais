"use client";

import { useState } from "react";
import { ArrowLeftRight, Plus, ReceiptText } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { DatePicker } from "@/components/date-picker";
import { EmptyState } from "@/components/empty-state";
import { ErrorState } from "@/components/error-state";
import { MovimentacaoDialog } from "@/components/forms/movimentacao-dialog";
import { TransferenciaDialog } from "@/components/forms/transferencia-dialog";
import { Money } from "@/components/money";
import { MovimentacoesTable } from "@/components/movimentacoes";
import { PageHeader } from "@/components/page-header";
import { StatCard } from "@/components/stat-card";
import { useMovimentacoes } from "@/lib/queries";
import { plural } from "@/lib/format";
import type { TipoMovimentacao } from "@/lib/types";

type FiltroTipo = "TODOS" | TipoMovimentacao;

export default function MovimentacoesPage() {
  const [inicio, setInicio] = useState<string | undefined>();
  const [fim, setFim] = useState<string | undefined>();
  const [tipo, setTipo] = useState<FiltroTipo>("TODOS");
  const [lancando, setLancando] = useState(false);
  const [transferindo, setTransferindo] = useState(false);

  const periodoCompleto = !!inicio && !!fim;
  const periodoIncompleto = !!inicio !== !!fim;

  const { data: movimentacoes = [], isLoading, error, refetch } = useMovimentacoes({
    inicio: periodoCompleto ? inicio : undefined,
    fim: periodoCompleto ? fim : undefined,
    tipo: tipo === "TODOS" ? undefined : tipo,
  });

  const entradas = movimentacoes.filter((m) => m.tipo === "RECEITA");
  const saidas = movimentacoes.filter((m) => m.tipo === "DESPESA");

  return (
    <div className="grid gap-8">
      <PageHeader
        title="Movimentações"
        description="Movimentações não são editadas. Para corrigir uma, faça o estorno."
        actions={
          <>
            <Button variant="outline" onClick={() => setTransferindo(true)}>
              <ArrowLeftRight />
              Transferir
            </Button>
            <Button onClick={() => setLancando(true)}>
              <Plus />
              Lançamento avulso
            </Button>
          </>
        }
      />

      <div className="grid gap-4 sm:grid-cols-3">
        <StatCard label="Lançamentos" value={movimentacoes.length} loading={isLoading} hint="No filtro atual" />
        <StatCard
          label="Entradas"
          value={<Money value={entradas.reduce((t, m) => t + m.valor, 0)} />}
          loading={isLoading}
          hint={plural(entradas.length, "lançamento", "lançamentos")}
        />
        <StatCard
          label="Saídas"
          value={<Money value={saidas.reduce((t, m) => t + m.valor, 0)} />}
          loading={isLoading}
          hint={plural(saidas.length, "lançamento", "lançamentos")}
        />
      </div>

      <div className="overflow-hidden rounded-2xl border bg-card">
        <div className="flex flex-col gap-3 border-b p-4 lg:flex-row lg:items-center lg:justify-between">
          <Tabs value={tipo} onValueChange={(v) => setTipo(v as FiltroTipo)}>
            <TabsList>
              <TabsTrigger value="TODOS" className="px-3">Todas</TabsTrigger>
              <TabsTrigger value="RECEITA" className="px-3">Entradas</TabsTrigger>
              <TabsTrigger value="DESPESA" className="px-3">Saídas</TabsTrigger>
            </TabsList>
          </Tabs>
          <div className="flex flex-col gap-1.5">
            <div className="flex flex-wrap items-center gap-2">
              <div className="w-48">
                <DatePicker value={inicio} onChange={setInicio} placeholder="Data inicial" clearable />
              </div>
              <span className="text-sm text-muted-foreground">até</span>
              <div className="w-48">
                <DatePicker value={fim} onChange={setFim} placeholder="Data final" clearable />
              </div>
            </div>
            {periodoIncompleto && (
              <p className="text-xs text-alerta">Informe as duas datas para filtrar por período.</p>
            )}
          </div>
        </div>

        {error ? (
          <ErrorState error={error} onRetry={refetch} />
        ) : !isLoading && movimentacoes.length === 0 ? (
          <EmptyState
            icon={ReceiptText}
            title="Nenhuma movimentação encontrada"
            description="Pagamentos e recebimentos de títulos, lançamentos avulsos e transferências aparecem aqui."
          />
        ) : (
          <MovimentacoesTable movimentacoes={movimentacoes} loading={isLoading} />
        )}
      </div>

      <MovimentacaoDialog open={lancando} onOpenChange={setLancando} />
      <TransferenciaDialog open={transferindo} onOpenChange={setTransferindo} />
    </div>
  );
}
