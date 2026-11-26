import type { SituacaoEmAberto, SituacaoTituloEfetiva, TipoMovimentacao } from "@/lib/types";
import { cn } from "@/lib/utils";

type Tone = "receita" | "despesa" | "alerta" | "neutral" | "ink";

const tones: Record<Tone, string> = {
  receita: "bg-receita-soft text-receita ring-receita/20",
  despesa: "bg-despesa-soft text-despesa ring-despesa/20",
  alerta: "bg-alerta-soft text-alerta ring-alerta/25",
  neutral: "bg-secondary text-muted-foreground ring-foreground/10",
  ink: "bg-foreground text-background ring-foreground",
};

export function Pill({
  tone,
  children,
  className,
  dot = true,
}: {
  tone: Tone;
  children: React.ReactNode;
  className?: string;
  dot?: boolean;
}) {
  return (
    <span
      className={cn(
        "inline-flex h-6 items-center gap-1.5 rounded-full px-2.5 text-xs font-medium whitespace-nowrap ring-1 ring-inset",
        tones[tone],
        className,
      )}
    >
      {dot && <span className="size-1.5 rounded-full bg-current" />}
      {children}
    </span>
  );
}

export function TipoBadge({ tipo }: { tipo: TipoMovimentacao }) {
  return <Pill tone={tipo === "RECEITA" ? "receita" : "despesa"}>{tipo === "RECEITA" ? "Receita" : "Despesa"}</Pill>;
}

const situacaoEfetiva: Record<SituacaoTituloEfetiva, { label: string; tone: Tone }> = {
  PENDENTE: { label: "Pendente", tone: "neutral" },
  PAGO: { label: "Quitado", tone: "receita" },
  VENCIDO: { label: "Vencido", tone: "despesa" },
  CANCELADO: { label: "Cancelado", tone: "neutral" },
};

export function SituacaoBadge({ situacao }: { situacao: SituacaoTituloEfetiva }) {
  const item = situacaoEfetiva[situacao];
  return (
    <Pill tone={item.tone} className={cn(situacao === "CANCELADO" && "line-through decoration-1")}>
      {item.label}
    </Pill>
  );
}

const situacaoAberto: Record<SituacaoEmAberto, { label: string; tone: Tone }> = {
  A_VENCER: { label: "A vencer", tone: "neutral" },
  VENCIDO: { label: "Vencido", tone: "despesa" },
  PARCIAL: { label: "Parcial", tone: "alerta" },
  PARCIAL_VENCIDO: { label: "Parcial vencido", tone: "despesa" },
};

export function SituacaoAbertoBadge({ situacao }: { situacao: SituacaoEmAberto }) {
  const item = situacaoAberto[situacao];
  return <Pill tone={item.tone}>{item.label}</Pill>;
}
