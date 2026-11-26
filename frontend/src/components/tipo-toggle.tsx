"use client";

import { ArrowDownLeft, ArrowUpRight } from "lucide-react";
import type { TipoMovimentacao } from "@/lib/types";
import { cn } from "@/lib/utils";

interface TipoToggleProps {
  value: TipoMovimentacao | undefined;
  onChange: (value: TipoMovimentacao) => void;
  disabled?: boolean;
  labels?: Partial<Record<TipoMovimentacao, string>>;
}

export function TipoToggle({ value, onChange, disabled, labels }: TipoToggleProps) {
  const opcoes: { tipo: TipoMovimentacao; label: string; icon: typeof ArrowDownLeft }[] = [
    { tipo: "RECEITA", label: labels?.RECEITA ?? "Receita", icon: ArrowDownLeft },
    { tipo: "DESPESA", label: labels?.DESPESA ?? "Despesa", icon: ArrowUpRight },
  ];

  return (
    <div className="grid grid-cols-2 gap-2" role="radiogroup">
      {opcoes.map(({ tipo, label, icon: Icon }) => {
        const ativo = value === tipo;
        return (
          <button
            key={tipo}
            type="button"
            role="radio"
            aria-checked={ativo}
            disabled={disabled}
            onClick={() => onChange(tipo)}
            className={cn(
              "flex h-11 items-center gap-2.5 rounded-xl border bg-card px-3 text-sm font-medium transition-all disabled:cursor-not-allowed disabled:opacity-60",
              ativo && tipo === "RECEITA" && "border-receita/40 bg-receita-soft text-receita ring-3 ring-receita/10",
              ativo && tipo === "DESPESA" && "border-despesa/40 bg-despesa-soft text-despesa ring-3 ring-despesa/10",
              !ativo && "text-muted-foreground hover:bg-muted hover:text-foreground",
            )}
          >
            <span
              className={cn(
                "grid size-6 place-items-center rounded-md",
                ativo ? "bg-current/10" : "bg-muted",
              )}
            >
              <Icon className="size-3.5" />
            </span>
            {label}
          </button>
        );
      })}
    </div>
  );
}
