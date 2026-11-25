"use client";

import { Controller, useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { ArrowDown } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { DatePicker } from "@/components/date-picker";
import { Field } from "@/components/field";
import { MoneyInput } from "@/components/money-input";
import { Spinner } from "@/components/spinner";
import { ContaSelect } from "@/components/forms/selects";
import { today } from "@/lib/format";
import { useContas, useTransferir } from "@/lib/queries";

const schema = z
  .object({
    contaOrigemId: z.number({ message: "A conta de origem é obrigatória" }),
    contaDestinoId: z.number({ message: "A conta de destino é obrigatória" }),
    valor: z.number({ message: "O valor é obrigatório" }).positive("O valor deve ser maior que zero"),
    data: z.string().min(1, "A data da transferência é obrigatória"),
    descricao: z.string().max(200, "A descrição deve ter no máximo 200 caracteres").optional(),
  })
  .refine((data) => data.contaOrigemId !== data.contaDestinoId, {
    message: "A conta de destino deve ser diferente da origem",
    path: ["contaDestinoId"],
  });

type FormData = z.infer<typeof schema>;

interface TransferenciaDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  contaOrigemInicial?: number;
}

export function TransferenciaDialog({ open, onOpenChange, contaOrigemInicial }: TransferenciaDialogProps) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Transferir entre contas</DialogTitle>
          <DialogDescription>
            Gera uma saída na origem e uma entrada no destino. Transferências não afetam os resultados.
          </DialogDescription>
        </DialogHeader>
        <TransferenciaForm contaOrigemInicial={contaOrigemInicial} onDone={() => onOpenChange(false)} />
      </DialogContent>
    </Dialog>
  );
}

function TransferenciaForm({ contaOrigemInicial, onDone }: { contaOrigemInicial?: number; onDone: () => void }) {
  const transferir = useTransferir();
  const { data: contas = [] } = useContas();
  const {
    register,
    control,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: { contaOrigemId: contaOrigemInicial, data: today(), descricao: "" },
  });
  const origem = useWatch({ control, name: "contaOrigemId" });

  const onSubmit = handleSubmit((data) => {
    const descricao = data.descricao?.trim() || undefined;
    const nomeOrigem = contas.find((c) => c.idConta === data.contaOrigemId)?.nome ?? "";
    const nomeDestino = contas.find((c) => c.idConta === data.contaDestinoId)?.nome ?? "";
    const limite = Math.min(200, 255 - Math.max(" para ".length + nomeDestino.length, " de ".length + nomeOrigem.length));
    if (descricao && descricao.length > limite) {
      setError("descricao", { message: `Com estas contas, a descrição deve ter no máximo ${limite} caracteres` });
      return;
    }
    transferir.mutate({ ...data, descricao }, { onSuccess: onDone });
  });

  return (
    <form onSubmit={onSubmit} className="grid gap-4" noValidate>
      <div className="relative grid gap-3 rounded-xl border bg-secondary/40 p-3">
        <Field label="De" htmlFor="transf-origem" error={errors.contaOrigemId?.message}>
          <Controller
            control={control}
            name="contaOrigemId"
            render={({ field }) => (
              <ContaSelect id="transf-origem" value={field.value} onChange={field.onChange} invalid={!!errors.contaOrigemId} placeholder="Conta de origem" />
            )}
          />
        </Field>
        <div className="flex justify-center">
          <span className="grid size-7 place-items-center rounded-full border bg-card text-muted-foreground shadow-sm">
            <ArrowDown className="size-3.5" />
          </span>
        </div>
        <Field label="Para" htmlFor="transf-destino" error={errors.contaDestinoId?.message}>
          <Controller
            control={control}
            name="contaDestinoId"
            render={({ field }) => (
              <ContaSelect
                id="transf-destino"
                value={field.value}
                onChange={field.onChange}
                invalid={!!errors.contaDestinoId}
                excluir={origem}
                placeholder="Conta de destino"
              />
            )}
          />
        </Field>
      </div>
      <div className="grid gap-4 sm:grid-cols-2">
        <Field label="Valor" htmlFor="transf-valor" error={errors.valor?.message}>
          <Controller
            control={control}
            name="valor"
            render={({ field }) => (
              <MoneyInput id="transf-valor" value={field.value} onChange={field.onChange} aria-invalid={!!errors.valor} />
            )}
          />
        </Field>
        <Field label="Data" htmlFor="transf-data" error={errors.data?.message}>
          <Controller
            control={control}
            name="data"
            render={({ field }) => (
              <DatePicker id="transf-data" value={field.value || undefined} onChange={(v) => field.onChange(v ?? "")} invalid={!!errors.data} />
            )}
          />
        </Field>
      </div>
      <Field label="Descrição" htmlFor="transf-descricao" error={errors.descricao?.message}>
        <Input id="transf-descricao" placeholder="Opcional" {...register("descricao")} />
      </Field>
      <DialogFooter className="mt-2">
        <Button type="button" variant="outline" onClick={onDone}>
          Cancelar
        </Button>
        <Button type="submit" disabled={transferir.isPending}>
          {transferir.isPending && <Spinner className="size-4 text-current" />}
          Transferir
        </Button>
      </DialogFooter>
    </form>
  );
}
