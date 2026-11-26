import { formatMoney } from "@/lib/format";
import type { TipoMovimentacao } from "@/lib/types";
import { cn } from "@/lib/utils";

interface MoneyProps {
  value: number | null | undefined;
  tipo?: TipoMovimentacao;
  colored?: boolean;
  className?: string;
}

export function Money({ value, tipo, colored, className }: MoneyProps) {
  const numero = Number(value ?? 0);
  const sinal = tipo === "DESPESA" ? "−" : tipo === "RECEITA" ? "+" : "";
  const cor = colored
    ? tipo === "DESPESA" || (!tipo && numero < 0)
      ? "text-despesa"
      : tipo === "RECEITA" || (!tipo && numero > 0)
        ? "text-receita"
        : ""
    : "";

  return (
    <span className={cn("tabular whitespace-nowrap", cor, className)}>
      {sinal && <span className="mr-0.5">{sinal}</span>}
      {formatMoney(tipo ? Math.abs(numero) : numero)}
    </span>
  );
}
