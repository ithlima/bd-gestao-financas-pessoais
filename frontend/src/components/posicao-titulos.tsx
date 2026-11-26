"use client";

import { useState } from "react";
import { HandCoins } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Skeleton } from "skullet/react";
import { LinhasCarregando } from "@/components/linhas-carregando";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { DatePicker } from "@/components/date-picker";
import { EmptyState } from "@/components/empty-state";
import { ErrorState } from "@/components/error-state";
import { LiquidarDialog, type TituloParaLiquidar } from "@/components/forms/liquidar-dialog";
import { Money } from "@/components/money";
import { PageHeader } from "@/components/page-header";
import { StatCard } from "@/components/stat-card";
import { SituacaoAbertoBadge } from "@/components/status-badge";
import { formatDate, plural } from "@/lib/format";
import { usePosicao } from "@/lib/queries";
import type { TipoMovimentacao, TituloEmAberto } from "@/lib/types";
import { cn } from "@/lib/utils";

type Recorte = "todos" | "vencidos" | "proximos";

function textoDias(dias: number) {
  if (dias > 0) return `${dias} ${dias === 1 ? "dia" : "dias"} em atraso`;
  if (dias === 0) return "Vence hoje";
  const faltam = Math.abs(dias);
  return `Em ${faltam} ${faltam === 1 ? "dia" : "dias"}`;
}

export function PosicaoTitulosView({ tipo }: { tipo: TipoMovimentacao }) {
  const despesa = tipo === "DESPESA";
  const [dataReferencia, setDataReferencia] = useState<string | undefined>();
  const [janelaTexto, setJanelaTexto] = useState("30");
  const [recorte, setRecorte] = useState<Recorte>("todos");
  const [liquidando, setLiquidando] = useState<TituloParaLiquidar | null>(null);

  const janelaDias = janelaTexto === "" ? undefined : Number(janelaTexto);
  const { data, isLoading, error, refetch } = usePosicao(tipo, { dataReferencia, janelaDias });

  const lista: TituloEmAberto[] =
    recorte === "vencidos" ? (data?.vencidos ?? []) : recorte === "proximos" ? (data?.aVencerProximosDias ?? []) : (data?.titulos ?? []);

  return (
    <div className="grid gap-8">
      <PageHeader
        title={despesa ? "Contas a pagar" : "Contas a receber"}
        description="Os totais somam o valor em aberto, não o previsto."
        actions={
          <div className="flex flex-wrap items-end gap-2">
            <div className="grid gap-1">
              <span className="text-[11px] font-medium text-muted-foreground">Data de referência</span>
              <div className="w-52">
                <DatePicker value={dataReferencia} onChange={setDataReferencia} placeholder="Hoje" clearable />
              </div>
            </div>
            <div className="grid gap-1">
              <span className="text-[11px] font-medium text-muted-foreground">Janela (dias)</span>
              <Input
                inputMode="numeric"
                aria-label="Janela (dias)"
                value={janelaTexto}
                onChange={(e) => setJanelaTexto(e.target.value.replace(/\D/g, "").slice(0, 4))}
                className="w-28 tabular"
              />
            </div>
          </div>
        }
      />

      {error ? (
        <ErrorState error={error} onRetry={refetch} />
      ) : (
        <>
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <StatCard
              label="Total em aberto"
              loading={isLoading}
              value={<Money value={data?.totalEmAberto} />}
              hint={plural(data?.quantidadeTitulos ?? 0, "título pendente", "títulos pendentes")}
            />
            <StatCard
              label="Vencido"
              loading={isLoading}
              value={<Money value={data?.totalVencido} />}
              hint={plural(data?.quantidadeVencidos ?? 0, "título vencido", "títulos vencidos")}
            />
            <StatCard
              label={`Próximos ${data?.janelaDias ?? janelaDias ?? 30} dias`}
              loading={isLoading}
              value={<Money value={data?.totalAVencerProximosDias} />}
              hint={plural(data?.quantidadeAVencerProximosDias ?? 0, "título a vencer", "títulos a vencer")}
            />
            <StatCard
              label="Parciais"
              loading={isLoading}
              value={data?.quantidadeParciais ?? 0}
              hint={`Referência: ${formatDate(data?.dataReferencia)}`}
            />
          </div>

          <div className="overflow-hidden rounded-2xl border bg-card">
            <div className="border-b p-4">
              <Tabs value={recorte} onValueChange={(v) => setRecorte(v as Recorte)}>
                <TabsList>
                  <TabsTrigger value="todos" className="px-3">
                    Todos <span className="ml-1 text-muted-foreground tabular">{data?.quantidadeTitulos ?? 0}</span>
                  </TabsTrigger>
                  <TabsTrigger value="vencidos" className="px-3">
                    Vencidos <span className="ml-1 text-muted-foreground tabular">{data?.quantidadeVencidos ?? 0}</span>
                  </TabsTrigger>
                  <TabsTrigger value="proximos" className="px-3">
                    Próximos dias <span className="ml-1 text-muted-foreground tabular">{data?.quantidadeAVencerProximosDias ?? 0}</span>
                  </TabsTrigger>
                </TabsList>
              </Tabs>
            </div>

            {!isLoading && lista.length === 0 ? (
              <EmptyState
                icon={HandCoins}
                title="Nada por aqui"
                description={despesa ? "Nenhuma conta a pagar neste recorte." : "Nenhuma conta a receber neste recorte."}
              />
            ) : (
              <Skeleton id={`posicao-${tipo}`} loading={isLoading}>
                <Table>
                  <TableHeader>
                    <TableRow className="hover:bg-transparent">
                      <TableHead className="pl-5">Título</TableHead>
                      <TableHead>Vencimento</TableHead>
                      <TableHead className="hidden text-right md:table-cell">Previsto</TableHead>
                      <TableHead className="hidden text-right lg:table-cell">Realizado</TableHead>
                      <TableHead className="text-right">Em aberto</TableHead>
                      <TableHead>Situação</TableHead>
                      <TableHead className="w-28 pr-5" />
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {isLoading && lista.length === 0 && (
                      <LinhasCarregando celulas={["pl-5", "", "hidden md:table-cell", "hidden lg:table-cell", "", "", "pr-5"]} />
                    )}
                    {lista.map((titulo) => {
                      const atrasado = titulo.diasParaVencimento > 0;
                      return (
                        <TableRow key={titulo.idTitulo}>
                          <TableCell className="pl-5">
                            <p className="max-w-[280px] truncate font-medium" title={titulo.descricao}>
                              {titulo.descricao}
                            </p>
                            <p className="text-xs text-muted-foreground">{titulo.categoriaNome}</p>
                          </TableCell>
                          <TableCell>
                            <p className="tabular">{formatDate(titulo.dataVencimento, "dd/MM/yyyy")}</p>
                            <p className={cn("text-xs", atrasado ? "text-despesa" : "text-muted-foreground")}>
                              {textoDias(titulo.diasParaVencimento)}
                            </p>
                          </TableCell>
                          <TableCell className="hidden text-right text-muted-foreground md:table-cell">
                            <Money value={titulo.valorPrevisto} />
                          </TableCell>
                          <TableCell className="hidden text-right text-muted-foreground lg:table-cell">
                            <Money value={titulo.valorRealizado} />
                          </TableCell>
                          <TableCell className="text-right font-semibold">
                            <Money value={titulo.valorEmAberto} />
                          </TableCell>
                          <TableCell>
                            <SituacaoAbertoBadge situacao={titulo.situacao} />
                          </TableCell>
                          <TableCell className="pr-5 text-right">
                            <Button size="sm" variant="outline" onClick={() => setLiquidando(titulo)}>
                              {despesa ? "Pagar" : "Receber"}
                            </Button>
                          </TableCell>
                        </TableRow>
                      );
                    })}
                  </TableBody>
                </Table>
              </Skeleton>
            )}
          </div>
        </>
      )}

      <LiquidarDialog titulo={liquidando} onOpenChange={(open) => !open && setLiquidando(null)} />
    </div>
  );
}
