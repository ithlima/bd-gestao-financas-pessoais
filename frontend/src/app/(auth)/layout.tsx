"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { Brand } from "@/components/brand";
import { Spinner } from "@/components/spinner";
import { useAuth } from "@/lib/auth";

export default function AuthLayout({ children }: { children: React.ReactNode }) {
  const { status } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (status === "autenticado") router.replace("/painel");
  }, [status, router]);

  if (status !== "anonimo") {
    return (
      <div className="grid min-h-screen place-items-center">
        <Spinner />
      </div>
    );
  }

  return (
    <div className="grid min-h-screen lg:grid-cols-[1.05fr_1fr]">
      <aside className="relative hidden overflow-hidden bg-ink text-ink-foreground lg:flex lg:flex-col lg:justify-between lg:p-12">
        <div className="bg-grid absolute inset-0 [mask-image:radial-gradient(ellipse_at_top_left,black_30%,transparent_75%)]" />
        <div className="absolute -right-32 -bottom-32 size-[28rem] rounded-full bg-brand/20 blur-3xl" />
        <Brand inverted className="relative" />

        <div className="relative max-w-md">
          <h2 className="text-4xl leading-[1.1] font-semibold tracking-tight text-white">
            O que você planeja e o que de fato acontece, lado a lado.
          </h2>
          <p className="mt-4 text-sm leading-relaxed text-white/60">
            Títulos registram compromissos futuros. Movimentações registram o que entrou e saiu do caixa.
            Os resultados comparam os dois.
          </p>

          <div className="mt-10 grid gap-3">
            <FeatureRow title="Títulos previstos" text="Contas a pagar e a receber com vencimento" />
            <FeatureRow title="Baixas parciais ou totais" text="Pagamentos e recebimentos vinculados" />
            <FeatureRow title="Transferências neutras" text="Movimente entre contas sem afetar os resultados" />
            <FeatureRow title="Saldo calculado" text="Derivado das movimentações realizadas" />
          </div>
        </div>

        <p className="relative text-xs text-white/40">Sistema de gestão de finanças pessoais</p>
      </aside>

      <main className="flex flex-col justify-center px-5 py-12 sm:px-10">
        <div className="mx-auto w-full max-w-sm">
          <Brand className="mb-10 lg:hidden" />
          {children}
        </div>
      </main>
    </div>
  );
}

function FeatureRow({ title, text }: { title: string; text: string }) {
  return (
    <div className="rounded-xl border border-white/8 bg-white/[0.03] px-4 py-3 backdrop-blur">
      <p className="text-sm font-medium text-white">{title}</p>
      <p className="text-xs text-white/50">{text}</p>
    </div>
  );
}
