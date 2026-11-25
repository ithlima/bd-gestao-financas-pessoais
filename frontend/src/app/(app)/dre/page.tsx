"use client";

import { useState } from "react";
import { Bar, BarChart, CartesianGrid, XAxis, YAxis } from "recharts";
import { ChartColumnBig } from "lucide-react";
import {
  ChartContainer,
  ChartLegend,
  ChartLegendContent,
  ChartTooltip,
  ChartTooltipContent,
  type ChartConfig,
} from "@/components/ui/chart";
import { Skeleton } from "skullet/react";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { DatePicker } from "@/components/date-picker";
import { EmptyState } from "@/components/empty-state";
import { ErrorState } from "@/components/error-state";
import { Money } from "@/components/money";
import { PageHeader } from "@/components/page-header";
import { StatCard } from "@/components/stat-card";
import { Pill, TipoBadge } from "@/components/status-badge";
import { firstDayOfMonth, formatMoney, lastDayOfMonth, plural } from "@/lib/format";
import { useDre } from "@/lib/queries";
import type { DreLinha, ModoDre } from "@/lib/types";
import { cn } from "@/lib/utils";

const chartConfig = {
  previsto: { label: "Previsto", color: "color-mix(in oklch, var(--foreground) 22%, transparent)" },
  realizado: { label: "Realizado", color: "var(--foreground)" },
} satisfies ChartConfig;

function Variacao({ valor }: { valor: number | null }) {
  if (valor === null) return <span className="text-muted-foreground">—</span>;
  const favoravel = valor >= 0;
  return (
    <span className={cn("tabular font-medium", favoravel ? "text-receita" : "text-despesa")}>
      {valor > 0 ? "+" : valor < 0 ? "−" : ""}
      {formatMoney(Math.abs(valor))}
    </span>
  );
}

function Valor({ valor }: { valor: number | null }) {
  if (valor === null) return <span className="text-muted-foreground">—</span>;
  return <Money value={valor} />;
}

export default function DrePage() {
  const [inicio, setInicio] = useState<string>(firstDayOfMonth());
  const [fim, setFim] = useState<string>(lastDayOfMonth());
  const [modo, setModo] = useState<ModoDre>("comparativo");
  const { data: dre, isLoading, error, refetch } = useDre(inicio, fim, modo);

  const mostrarPrevisto = modo !== "realizado";
  const mostrarRealizado = modo !== "previsto";
  const comparativo = modo === "comparativo";

  const dadosGrafico = [...(dre?.linhasReceitas ?? []), ...(dre?.linhasDespesas ?? [])].map((linha) => ({
    categoria: linha.categoriaNome,
    previsto: linha.valorPrevisto ?? 0,
    realizado: linha.valorRealizado ?? 0,
  }));

  return (
    <div className="grid grid-cols-1 gap-8">
      <PageHeader
        title="Resultados"
        description="Previsto usa a data de vencimento dos títulos. Realizado usa a data das movimentações, sem transferências."
      />

      <div className="flex flex-col gap-3 rounded-2xl border bg-card p-4 lg:flex-row lg:items-center lg:justify-between">
        <Tabs value={modo} onValueChange={(v) => setModo(v as ModoDre)}>
          <TabsList className="w-full sm:w-fit">
            <TabsTrigger value="comparativo" className="px-2 sm:px-4">
              Comparativo
            </TabsTrigger>
            <TabsTrigger value="previsto" className="px-2 sm:px-4">
              Previsto
            </TabsTrigger>
            <TabsTrigger value="realizado" className="px-2 sm:px-4">
              Realizado
            </TabsTrigger>
          </TabsList>
        </Tabs>
        <div className="flex flex-wrap items-center gap-2">
          <div className="w-48">
            <DatePicker value={inicio} onChange={(v) => v && setInicio(v)} placeholder="Início" />
          </div>
          <span className="text-sm text-muted-foreground">até</span>
          <div className="w-48">
            <DatePicker value={fim} onChange={(v) => v && setFim(v)} placeholder="Fim" />
          </div>
        </div>
      </div>

      {error ? (
        <div className="rounded-2xl border bg-card">
          <ErrorState error={error} onRetry={refetch} />
        </div>
      ) : (
        <>
          <div className={cn("grid gap-4 sm:grid-cols-2", comparativo ? "xl:grid-cols-4" : "xl:grid-cols-3")}>
            <StatCard
              label="Receitas"
              loading={isLoading}
              value={<Valor valor={mostrarRealizado ? (dre?.totalReceitasRealizadas ?? null) : (dre?.totalReceitasPrevistas ?? null)} />}
              hint={
                comparativo && (
                  <>
                    Previsto <Money value={dre?.totalReceitasPrevistas} />
                  </>
                )
              }
            />
            <StatCard
              label="Despesas"
              loading={isLoading}
              value={<Valor valor={mostrarRealizado ? (dre?.totalDespesasRealizadas ?? null) : (dre?.totalDespesasPrevistas ?? null)} />}
              hint={
                comparativo && (
                  <>
                    Previsto <Money value={dre?.totalDespesasPrevistas} />
                  </>
                )
              }
            />
            <StatCard
              label="Resultado"
              loading={isLoading}
              value={
                <span className={cn(((mostrarRealizado ? dre?.resultadoRealizado : dre?.resultadoPrevisto) ?? 0) < 0 && "text-despesa")}>
                  <Valor valor={mostrarRealizado ? (dre?.resultadoRealizado ?? null) : (dre?.resultadoPrevisto ?? null)} />
                </span>
              }
              hint={
                comparativo && (
                  <>
                    Previsto <Money value={dre?.resultadoPrevisto} />
                  </>
                )
              }
            />
            {comparativo && (
              <StatCard
                label="Variação do resultado"
                loading={isLoading}
                value={<Variacao valor={dre?.variacaoResultado ?? null} />}
                hint={
                  dre && (
                    <Pill tone={dre.variacaoFavoravel ? "receita" : "despesa"}>{dre.variacaoFavoravel ? "Favorável" : "Desfavorável"}</Pill>
                  )
                }
              />
            )}
          </div>

          <div className="grid grid-cols-1 gap-6 xl:grid-cols-[1.4fr_1fr]">
            <section className="min-w-0 rounded-2xl border bg-card p-5">
              <p className="mb-4 font-medium">Por categoria</p>
              <Skeleton id="resultados-1" loading={isLoading}>
                {dadosGrafico.length === 0 ? (
                  <EmptyState
                    icon={ChartColumnBig}
                    title="Sem dados no período"
                    description="Nenhum título ou movimentação classificada neste intervalo."
                  />
                ) : (
                  <div className="-mx-5 overflow-x-auto px-5">
                    <ChartContainer config={chartConfig} className="h-72 w-full" style={{ minWidth: dadosGrafico.length * 72 }}>
                      <BarChart data={dadosGrafico} barGap={4} margin={{ left: 4, right: 4 }}>
                        <CartesianGrid vertical={false} />
                        <XAxis dataKey="categoria" tickLine={false} axisLine={false} tickMargin={10} interval={0} fontSize={11} />
                        <YAxis
                          tickLine={false}
                          axisLine={false}
                          width={64}
                          fontSize={11}
                          tickFormatter={(v: number) => new Intl.NumberFormat("pt-BR", { notation: "compact" }).format(v)}
                        />
                        <ChartTooltip
                          cursor={{ fill: "var(--muted)" }}
                          content={
                            <ChartTooltipContent
                              formatter={(value, name) => (
                                <div className="flex w-full items-center justify-between gap-4">
                                  <span className="text-muted-foreground">{chartConfig[name as keyof typeof chartConfig]?.label}</span>
                                  <span className="font-medium tabular">{formatMoney(Number(value))}</span>
                                </div>
                              )}
                            />
                          }
                        />
                        <ChartLegend align="left" content={<ChartLegendContent className="justify-start" />} />
                        {mostrarPrevisto && <Bar dataKey="previsto" fill="var(--color-previsto)" radius={[6, 6, 0, 0]} maxBarSize={28} />}
                        {mostrarRealizado && (
                          <Bar dataKey="realizado" fill="var(--color-realizado)" radius={[6, 6, 0, 0]} maxBarSize={28} />
                        )}
                      </BarChart>
                    </ChartContainer>
                  </div>
                )}
              </Skeleton>
            </section>

            <section className="flex min-w-0 flex-col rounded-2xl border bg-card">
              <header className="flex items-center justify-between border-b px-5 py-4">
                <div>
                  <p className="font-medium">Títulos vencidos</p>
                  <p className="text-xs text-muted-foreground">
                    {(dre?.titulosVencidos.length ?? 0) > 4 ? `4 de ${dre?.titulosVencidos.length} · ` : ""}Não alteram o resultado
                  </p>
                </div>
                <Skeleton id="resultados-2" loading={isLoading} label="Carregando total vencido">
                  <p className="text-sm font-semibold">
                    <Money value={dre?.totalVencido} />
                  </p>
                </Skeleton>
              </header>
              <Skeleton id="resultados-3" loading={isLoading}>
                {(dre?.titulosVencidos ?? []).length === 0 ? (
                  <p className="px-5 py-12 text-center text-sm text-muted-foreground">Nenhum título vencido.</p>
                ) : (
                  <ul className="divide-y">
                    {dre?.titulosVencidos.slice(0, 4).map((t) => (
                      <li key={t.idTitulo} className="flex items-center justify-between gap-3 px-5 py-3">
                        <div className="min-w-0">
                          <p className="truncate text-sm font-medium" title={t.descricao}>
                            {t.descricao}
                          </p>
                          <p className="text-xs text-despesa">{plural(t.diasDeAtraso, "dia", "dias")} de atraso</p>
                        </div>
                        <div className="flex flex-col items-end gap-1">
                          <span className="text-sm font-medium">
                            <Money value={t.valorPrevisto} />
                          </span>
                          <TipoBadge tipo={t.tipo} />
                        </div>
                      </li>
                    ))}
                  </ul>
                )}
              </Skeleton>
            </section>
          </div>

          <section className="overflow-x-auto rounded-2xl border bg-card">
            <div className="min-w-[640px]">
              <div className="grid grid-cols-[1fr_repeat(3,minmax(0,140px))] gap-x-4 border-b bg-secondary/50 px-5 py-3 text-xs font-medium text-muted-foreground">
                <span>Demonstração</span>
                <span className={cn("text-right", !mostrarPrevisto && "invisible")}>Previsto</span>
                <span className={cn("text-right", !mostrarRealizado && "invisible")}>Realizado</span>
                <span className={cn("text-right", !comparativo && "invisible")}>Variação</span>
              </div>
              <Skeleton id="resultados-4" loading={isLoading}>
                {dre ? (
                  <>
                    <Secao titulo="Receitas" linhas={dre.linhasReceitas} />
                    <TotalLinha label="Total de receitas" previsto={dre.totalReceitasPrevistas} realizado={dre.totalReceitasRealizadas} />
                    <Secao titulo="Despesas" linhas={dre.linhasDespesas} />
                    <TotalLinha label="Total de despesas" previsto={dre.totalDespesasPrevistas} realizado={dre.totalDespesasRealizadas} />
                    <div className="grid grid-cols-[1fr_repeat(3,minmax(0,140px))] items-center gap-x-4 border-t-2 border-foreground/80 bg-muted/60 px-5 py-4">
                      <span className="font-semibold">Resultado do período</span>
                      <span className="text-right font-semibold">
                        {dre.resultadoPrevisto !== null ? <Money value={dre.resultadoPrevisto} /> : ""}
                      </span>
                      <span className="text-right font-semibold">
                        {dre.resultadoRealizado !== null ? <Money value={dre.resultadoRealizado} /> : ""}
                      </span>
                      <span className="text-right">
                        {dre.variacaoResultado !== null && (
                          <span className={cn("tabular font-semibold", dre.variacaoFavoravel ? "text-receita" : "text-despesa")}>
                            {dre.variacaoResultado > 0 ? "+" : dre.variacaoResultado < 0 ? "−" : ""}
                            {formatMoney(Math.abs(dre.variacaoResultado))}
                          </span>
                        )}
                      </span>
                    </div>
                  </>
                ) : null}
              </Skeleton>
            </div>
          </section>
        </>
      )}
    </div>
  );
}

function Secao({ titulo, linhas }: { titulo: string; linhas: DreLinha[] }) {
  return (
    <div>
      <p className="px-5 pt-5 pb-2 text-[11px] font-semibold tracking-[0.14em] text-muted-foreground uppercase">{titulo}</p>
      {linhas.length === 0 ? (
        <p className="px-5 pb-4 text-sm text-muted-foreground">Nenhuma categoria com valores no período.</p>
      ) : (
        <ul>
          {linhas.map((linha) => (
            <li
              key={`${linha.tipo}-${linha.categoriaId}`}
              className="grid grid-cols-[1fr_repeat(3,minmax(0,140px))] items-center gap-x-4 px-5 py-2.5 text-sm hover:bg-muted/40"
            >
              <span>{linha.categoriaNome}</span>
              <span className="text-right text-muted-foreground">
                {linha.valorPrevisto !== null && <Money value={linha.valorPrevisto} />}
              </span>
              <span className="text-right">{linha.valorRealizado !== null && <Money value={linha.valorRealizado} />}</span>
              <span className="text-right">{linha.variacao !== null && <Variacao valor={linha.variacao} />}</span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

function TotalLinha({ label, previsto, realizado }: { label: string; previsto: number | null; realizado: number | null }) {
  return (
    <div className="mt-1 grid grid-cols-[1fr_repeat(3,minmax(0,140px))] items-center gap-x-4 border-y bg-secondary/40 px-5 py-3 text-sm font-semibold">
      <span>{label}</span>
      <span className="text-right">{previsto !== null && <Money value={previsto} />}</span>
      <span className="text-right">{realizado !== null && <Money value={realizado} />}</span>
      <span />
    </div>
  );
}
