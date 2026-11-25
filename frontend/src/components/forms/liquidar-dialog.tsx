"use client";

import { Controller, useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { DatePicker } from "@/components/date-picker";
import { Field } from "@/components/field";
import { MoneyInput } from "@/components/money-input";
import { Spinner } from "@/components/spinner";
import { ContaSelect } from "@/components/forms/selects";
import { useUsuarioId } from "@/lib/auth";
import { formatDate, formatMoney, today } from "@/lib/format";
import { useLiquidarTitulo } from "@/lib/queries";
import type { TipoMovimentacao } from "@/lib/types";
import { cn } from "@/lib/utils";

export interface TituloParaLiquidar {
  idTitulo: number;
  descricao: string;
  tipo: TipoMovimentacao;
  valorPrevisto: number;
  valorRealizado: number;
  valorEmAberto: number;
  dataVencimento: string;
  categoriaNome?: string;
}

interface LiquidarDialogProps {
  titulo: TituloParaLiquidar | null;
  onOpenChange: (open: boolean) => void;
}

export function LiquidarDialog({ titulo, onOpenChange }: LiquidarDialogProps) {
  const despesa = titulo?.tipo === "DESPESA";
  return (
    <Dialog open={!!titulo} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>{despesa ? "Registrar pagamento" : "Registrar recebimento"}</DialogTitle>
          <DialogDescription>
            Gera uma movimentação realizada na conta escolhida. Valores parciais mantêm o título pendente.
          </DialogDescription>
        </DialogHeader>
        {titulo && <LiquidarForm titulo={titulo} onDone={() => onOpenChange(false)} />}
      </DialogContent>
    </Dialog>
  );
}

function LiquidarForm({ titulo, onDone }: { titulo: TituloParaLiquidar; onDone: () => void }) {
  const usuarioId = useUsuarioId();
  const liquidar = useLiquidarTitulo();
  const despesa = titulo.tipo === "DESPESA";

  const schema = z.object({
    valor: z
      .number({ message: "O valor é obrigatório" })
      .positive("O valor deve ser maior que zero")
      .max(titulo.valorEmAberto, `O valor não pode ultrapassar ${formatMoney(titulo.valorEmAberto)} em aberto`),
    data: z.string().min(1, despesa ? "A data do pagamento é obrigatória" : "A data do recebimento é obrigatória"),
    contaId: z.number({ message: "A conta é obrigatória" }),
  });

  const {
    control,
    handleSubmit,
    setValue,
    formState: { errors },
  } = useForm<z.infer<typeof schema>>({
    resolver: zodResolver(schema),
    defaultValues: { valor: titulo.valorEmAberto, data: today(), contaId: undefined },
  });

  const valor = useWatch({ control, name: "valor" }) ?? 0;
  const percentualRealizado = Math.min(100, (titulo.valorRealizado / titulo.valorPrevisto) * 100);
  const percentualNovo = Math.min(100 - percentualRealizado, (valor / titulo.valorPrevisto) * 100);
  const quita = valor > 0 && Math.abs(valor - titulo.valorEmAberto) < 0.005;

  const onSubmit = handleSubmit((data) => {
    liquidar.mutate(
      { id: titulo.idTitulo, tipo: titulo.tipo, data: { ...data, usuarioId } },
      {
        onSuccess: (resultado) => {
          toast.success(
            resultado.situacao === "PAGO"
              ? "Título quitado"
              : despesa
                ? "Pagamento parcial registrado"
                : "Recebimento parcial registrado",
            {
              description:
                resultado.situacao === "PAGO"
                  ? undefined
                  : `Restam ${formatMoney(resultado.valorEmAberto)} em aberto.`,
            },
          );
          onDone();
        },
      },
    );
  });

  return (
    <form onSubmit={onSubmit} className="grid gap-4" noValidate>
      <div className="rounded-xl border bg-secondary/50 p-4">
        <div className="flex items-start justify-between gap-3">
          <div className="min-w-0">
            <p className="truncate font-medium" title={titulo.descricao}>{titulo.descricao}</p>
            <p className="mt-0.5 text-xs text-muted-foreground">
              {titulo.categoriaNome ? `${titulo.categoriaNome} · ` : ""}vence {formatDate(titulo.dataVencimento)}
            </p>
          </div>
          <div className="text-right">
            <p className="text-[11px] text-muted-foreground uppercase">Em aberto</p>
            <p className="tabular font-semibold">{formatMoney(titulo.valorEmAberto)}</p>
          </div>
        </div>
        <div className="mt-4 flex h-2 overflow-hidden rounded-full bg-border">
          <div className="h-full bg-foreground/70" style={{ width: `${percentualRealizado}%` }} />
          <div
            className={cn("h-full transition-all", despesa ? "bg-despesa" : "bg-receita")}
            style={{ width: `${Math.max(0, percentualNovo)}%` }}
          />
        </div>
        <div className="mt-2 flex justify-between text-[11px] text-muted-foreground">
          <span className="tabular">Realizado {formatMoney(titulo.valorRealizado)}</span>
          <span className="tabular">Previsto {formatMoney(titulo.valorPrevisto)}</span>
        </div>
      </div>

      <Field
        label={despesa ? "Valor pago" : "Valor recebido"}
        htmlFor="liquidar-valor"
        error={errors.valor?.message}
        hint={quita ? "Este valor quita o título." : valor > 0 ? "Baixa parcial: o título continuará pendente." : undefined}
      >
        <div className="flex gap-2">
          <Controller
            control={control}
            name="valor"
            render={({ field }) => (
              <MoneyInput id="liquidar-valor" value={field.value} onChange={field.onChange} aria-invalid={!!errors.valor} />
            )}
          />
          <Button
            type="button"
            variant="outline"
            className="shrink-0"
            onClick={() => setValue("valor", titulo.valorEmAberto, { shouldValidate: true })}
          >
            Total
          </Button>
        </div>
      </Field>
      <div className="grid gap-4 sm:grid-cols-2">
        <Field label="Data" htmlFor="liquidar-data" error={errors.data?.message}>
          <Controller
            control={control}
            name="data"
            render={({ field }) => (
              <DatePicker id="liquidar-data" value={field.value || undefined} onChange={(v) => field.onChange(v ?? "")} invalid={!!errors.data} />
            )}
          />
        </Field>
        <Field label="Conta" htmlFor="liquidar-conta" error={errors.contaId?.message}>
          <Controller
            control={control}
            name="contaId"
            render={({ field }) => (
              <ContaSelect id="liquidar-conta" value={field.value} onChange={field.onChange} invalid={!!errors.contaId} />
            )}
          />
        </Field>
      </div>
      <DialogFooter className="mt-2">
        <Button type="button" variant="outline" onClick={onDone}>
          Cancelar
        </Button>
        <Button type="submit" disabled={liquidar.isPending}>
          {liquidar.isPending && <Spinner className="size-4 text-current" />}
          {despesa ? "Confirmar pagamento" : "Confirmar recebimento"}
        </Button>
      </DialogFooter>
    </form>
  );
}
