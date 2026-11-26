"use client";

import { useState } from "react";
import Link from "next/link";
import { HandCoins } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Skeleton } from "skullet/react";
import { DatePicker } from "@/components/date-picker";
import { ErrorState } from "@/components/error-state";
import { LiquidarDialog, type TituloParaLiquidar } from "@/components/forms/liquidar-dialog";
import { Money } from "@/components/money";
import { origemDe } from "@/components/movimentacoes";
import { Pill, SituacaoAbertoBadge } from "@/components/status-badge";
import { useAuth } from "@/lib/auth";
import { formatDate, formatMoney, labelTipoConta, plural } from "@/lib/format";
import { useContas, useMovimentacoes, useResumoFinanceiro, useTitulosVencidos } from "@/lib/queries";
import type { PosicaoTitulos } from "@/lib/types";
import { cn } from "@/lib/utils";

export default function PainelPage() {
  const { usuario } = useAuth();
  const [dataReferencia, setDataReferencia] = useState<string | undefined>();
  const [janelaTexto, setJanelaTexto] = useState("30");
  const janelaDias = janelaTexto === "" ? undefined : Number(janelaTexto);

  const resumo = useResumoFinanceiro({ dataReferencia, janelaDias });
  const contas = useContas();
  const vencidos = useTitulosVencidos(dataReferencia);
  const movimentacoes = useMovimentacoes();
  const [liquidando, setLiquidando] = useState<TituloParaLiquidar | null>(null);

  const saldoContas = (contas.data ?? []).reduce((t, c) => t + c.saldoAtual, 0);
  const proximos = [
    ...(resumo.data?.contasAPagar.aVencerProximosDias ?? []),
    ...(resumo.data?.contasAReceber.aVencerProximosDias ?? []),
  ].sort((a, b) => a.dataVencimento.localeCompare(b.dataVencimento));
  const ultimas = [...(movimentacoes.data ?? [])]
    .sort((a, b) => b.data.localeCompare(a.data) || b.idMovimentacao - a.idMovimentacao)
    .slice(0, 6);

  const primeiroNome = usuario?.nome.split(" ")[0] ?? "";

  return (
    <div className="grid gap-8">
      <div className="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
        <h1 className="text-2xl font-semibold tracking-tight md:text-[28px]">Olá, {primeiroNome}</h1>
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
      </div>

      {resumo.error ? (
        <div className="rounded-2xl border bg-card">
          <ErrorState error={resumo.error} onRetry={resumo.refetch} />
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
          <div className="@container flex min-w-0 flex-col rounded-2xl border bg-card p-5 md:col-span-2 xl:col-span-1">
            <p className="text-sm font-medium">Saldo previsto</p>
            <Skeleton id="painel-saldo-previsto" loading={resumo.isLoading} label="Carregando saldo previsto">
              <p
                className={cn(
                  "mt-4 text-[clamp(1.25rem,11cqi,1.625rem)] font-semibold tracking-tight whitespace-nowrap tabular",
                  (resumo.data?.saldoPrevisto ?? 0) < 0 && "text-despesa",
                )}
              >
                {formatMoney(resumo.data?.saldoPrevisto)}
              </p>
            </Skeleton>
            <p className="text-xs text-muted-foreground">A receber menos a pagar, em aberto</p>
            <div className="mt-auto flex flex-wrap items-center justify-between gap-x-3 gap-y-1 pt-4 text-xs">
              <span className="text-muted-foreground">
                Nas contas{" "}
                <Skeleton id="painel-saldo-contas" loading={contas.isLoading} label="Carregando saldo das contas" className="inline-block">
                  <span className="font-medium text-foreground tabular">{formatMoney(saldoContas)}</span>
                </Skeleton>
              </span>
              <Link href="/contas" className="font-medium text-foreground underline-offset-4 hover:underline">
                Ver contas
              </Link>
            </div>
          </div>

          <PosicaoCard
            titulo="Contas a receber"
            href="/contas-a-receber"
            posicao={resumo.data?.contasAReceber}
            loading={resumo.isLoading}
          />
          <PosicaoCard titulo="Contas a pagar" href="/contas-a-pagar" posicao={resumo.data?.contasAPagar} loading={resumo.isLoading} />

          <section className="min-w-0 overflow-hidden rounded-2xl border bg-card md:col-span-2 xl:col-span-3">
            <header className="flex items-center justify-between border-b px-5 py-4">
              <div>
                <p className="font-medium">Próximos vencimentos</p>
                <p className="text-xs text-muted-foreground">
                  {proximos.length > 6 ? `6 de ${proximos.length} títulos` : "Títulos"} a vencer nos próximos {resumo.data?.contasAPagar.janelaDias ?? janelaDias ?? 30} dias
                </p>
              </div>
              <Button variant="ghost" size="sm" asChild>
                <Link href="/titulos">Ver títulos</Link>
              </Button>
            </header>
            <Skeleton id="painel-3" loading={resumo.isLoading}>
              {proximos.length === 0 ? (
                <p className="px-5 py-12 text-center text-sm text-muted-foreground">Nenhum título a vencer nesta janela.</p>
              ) : (
                <ul className="divide-y">
                  {proximos.slice(0, 6).map((t) => (
                    <li key={t.idTitulo} className="flex items-center gap-4 px-5 py-3">
                      <div className="grid w-12 shrink-0 place-items-center rounded-xl border py-1.5 text-center">
                        <span className="text-base leading-none font-semibold tabular">{formatDate(t.dataVencimento, "dd")}</span>
                        <span className="mt-0.5 text-[10px] text-muted-foreground uppercase">{formatDate(t.dataVencimento, "MMM")}</span>
                      </div>
                      <div className="min-w-0 flex-1">
                        <p className="truncate text-sm font-medium" title={t.descricao}>
                          {t.descricao}
                        </p>
                        <p className="truncate text-xs text-muted-foreground" title={t.categoriaNome}>
                          {t.categoriaNome}
                        </p>
                      </div>
                      <div className="hidden sm:block">
                        <SituacaoAbertoBadge situacao={t.situacao} />
                      </div>
                      <p className="shrink-0 text-right text-sm font-semibold sm:w-28">
                        <Money value={t.valorEmAberto} tipo={t.tipo} colored />
                      </p>
                      <Button
                        variant="outline"
                        size="icon-sm"
                        aria-label={t.tipo === "DESPESA" ? "Pagar" : "Receber"}
                        onClick={() => setLiquidando(t)}
                      >
                        <HandCoins />
                      </Button>
                    </li>
                  ))}
                </ul>
              )}
            </Skeleton>
          </section>
        </div>
      )}

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
        <section className="min-w-0 overflow-hidden rounded-2xl border bg-card">
          <header className="flex items-center justify-between border-b px-5 py-4">
            <p className="font-medium">Contas</p>
            <Button variant="ghost" size="sm" asChild>
              <Link href="/contas">Gerenciar</Link>
            </Button>
          </header>
          <Skeleton id="painel-4" loading={contas.isLoading}>
            {(contas.data ?? []).length === 0 ? (
              <p className="px-5 py-10 text-center text-sm text-muted-foreground">Nenhuma conta cadastrada.</p>
            ) : (
              <ul className="divide-y">
                {contas.data?.map((conta) => (
                  <li key={conta.idConta}>
                    <Link
                      href={`/contas/${conta.idConta}`}
                      className="flex items-center justify-between gap-3 px-5 py-3 transition-colors hover:bg-muted/40"
                    >
                      <div className="min-w-0">
                        <p className="truncate text-sm font-medium" title={conta.nome}>
                          {conta.nome}
                        </p>
                        <p className="truncate text-xs text-muted-foreground" title={labelTipoConta(conta.tipo)}>
                          {labelTipoConta(conta.tipo)}
                        </p>
                      </div>
                      <span className={cn("text-sm font-semibold", conta.saldoAtual < 0 && "text-despesa")}>
                        <Money value={conta.saldoAtual} />
                      </span>
                    </Link>
                  </li>
                ))}
              </ul>
            )}
          </Skeleton>
        </section>

        <section className="min-w-0 overflow-hidden rounded-2xl border bg-card">
          <header className="flex items-center justify-between border-b px-5 py-4">
            <div className="flex items-center gap-2">
              <p className="font-medium">Títulos vencidos</p>
              <Skeleton id="painel-5" loading={vencidos.isLoading} label="Carregando títulos vencidos">
                <Pill tone={(vencidos.data ?? []).length > 0 ? "despesa" : "neutral"}>{(vencidos.data ?? []).length}</Pill>
              </Skeleton>
            </div>
            <Button variant="ghost" size="sm" asChild>
              <Link href="/titulos">Ver títulos</Link>
            </Button>
          </header>
          <Skeleton id="painel-6" loading={vencidos.isLoading}>
            {(vencidos.data ?? []).length === 0 ? (
              <p className="px-5 py-10 text-center text-sm text-muted-foreground">Nenhum título em atraso.</p>
            ) : (
              <ul className="divide-y">
                {vencidos.data?.slice(0, 6).map((t) => (
                  <li key={t.idTitulo} className="flex items-center justify-between gap-3 px-5 py-3">
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-medium" title={t.descricao}>
                        {t.descricao}
                      </p>
                      <p className="text-xs text-muted-foreground">Venceu em {formatDate(t.dataVencimento)}</p>
                    </div>
                    <span className="shrink-0 text-sm font-semibold whitespace-nowrap">
                      <Money value={t.valorEmAberto} tipo={t.tipo} colored />
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </Skeleton>
        </section>

        <section className="min-w-0 overflow-hidden rounded-2xl border bg-card md:col-span-2 xl:col-span-1">
          <header className="flex items-center justify-between border-b px-5 py-4">
            <p className="font-medium">Últimas movimentações</p>
            <Button variant="ghost" size="sm" asChild>
              <Link href="/movimentacoes">Ver todas</Link>
            </Button>
          </header>
          <Skeleton id="painel-7" loading={movimentacoes.isLoading}>
            {ultimas.length === 0 ? (
              <p className="px-5 py-10 text-center text-sm text-muted-foreground">Nenhuma movimentação registrada.</p>
            ) : (
              <ul className="divide-y">
                {ultimas.map((m) => (
                  <li key={m.idMovimentacao} className="flex items-center justify-between gap-3 px-5 py-3">
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-medium" title={m.descricao}>
                        {m.descricao}
                      </p>
                      <p className="truncate text-xs text-muted-foreground">
                        {formatDate(m.data, "dd/MM")} ·{" "}
                        {origemDe(m) === "titulo" ? `Título #${m.tituloId}` : origemDe(m) === "transferencia" ? "Transferência" : (m.categoriaNome ?? "Avulsa")}
                      </p>
                    </div>
                    <span className="shrink-0 text-sm font-semibold whitespace-nowrap">
                      <Money value={m.valor} tipo={m.tipo} colored />
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </Skeleton>
        </section>
      </div>

      <LiquidarDialog titulo={liquidando} onOpenChange={(open) => !open && setLiquidando(null)} />
    </div>
  );
}

function PosicaoCard({ titulo, href, posicao, loading }: { titulo: string; href: string; posicao?: PosicaoTitulos; loading: boolean }) {
  return (
    <Link
      href={href}
      className="@container flex min-w-0 flex-col rounded-2xl border bg-card p-5 transition-shadow hover:shadow-lg hover:shadow-foreground/5"
    >
      <p className="text-sm font-medium">{titulo}</p>
      <Skeleton id={`painel-posicao${href}`} loading={loading} label={`Carregando ${titulo.toLowerCase()}`} className="flex flex-1 flex-col">
        <p className="mt-4 text-[clamp(1.25rem,11cqi,1.625rem)] font-semibold tracking-tight whitespace-nowrap">
          <Money value={posicao?.totalEmAberto} />
        </p>
        <p className="text-xs text-muted-foreground">{plural(posicao?.quantidadeTitulos ?? 0, "título em aberto", "títulos em aberto")}</p>
        <div className="mt-auto flex flex-wrap justify-between gap-x-3 gap-y-1 pt-4 text-xs">
          <span className="text-muted-foreground">
            Vencido <span className="font-medium text-despesa tabular">{formatMoney(posicao?.totalVencido)}</span>
          </span>
          <span className="text-muted-foreground tabular">{plural(posicao?.quantidadeParciais ?? 0, "parcial", "parciais")}</span>
        </div>
      </Skeleton>
    </Link>
  );
}
