"use client";

import { Controller, useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
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
import { TipoToggle } from "@/components/tipo-toggle";
import { CategoriaSelect, ContaSelect } from "@/components/forms/selects";
import { useUsuarioId } from "@/lib/auth";
import { today } from "@/lib/format";
import { useRegistrarMovimentacao } from "@/lib/queries";

const schema = z.object({
  tipo: z.enum(["RECEITA", "DESPESA"], { message: "O tipo é obrigatório" }),
  descricao: z.string().trim().min(1, "A descrição é obrigatória").max(255, "A descrição deve ter no máximo 255 caracteres"),
  valor: z.number({ message: "O valor é obrigatório" }).positive("O valor deve ser maior que zero"),
  data: z.string().min(1, "A data é obrigatória"),
  contaId: z.number({ message: "A conta é obrigatória" }),
  categoriaId: z.number({ message: "A categoria é obrigatória" }),
});

type FormData = z.infer<typeof schema>;

interface MovimentacaoDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  contaInicial?: number;
}

export function MovimentacaoDialog({ open, onOpenChange, contaInicial }: MovimentacaoDialogProps) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>Lançamento avulso</DialogTitle>
          <DialogDescription>
            Registra uma entrada ou saída que já aconteceu, sem título previsto vinculado.
          </DialogDescription>
        </DialogHeader>
        <MovimentacaoForm contaInicial={contaInicial} onDone={() => onOpenChange(false)} />
      </DialogContent>
    </Dialog>
  );
}

function MovimentacaoForm({ contaInicial, onDone }: { contaInicial?: number; onDone: () => void }) {
  const usuarioId = useUsuarioId();
  const registrar = useRegistrarMovimentacao();
  const {
    register,
    control,
    handleSubmit,
    setValue,
    formState: { errors },
  } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: { tipo: "DESPESA", descricao: "", data: today(), contaId: contaInicial },
  });
  const tipo = useWatch({ control, name: "tipo" });

  const onSubmit = handleSubmit((data) => {
    registrar.mutate({ ...data, usuarioId }, { onSuccess: onDone });
  });

  return (
    <form onSubmit={onSubmit} className="grid gap-4" noValidate>
      <Field label="Tipo" error={errors.tipo?.message}>
        <Controller
          control={control}
          name="tipo"
          render={({ field }) => (
            <TipoToggle
              value={field.value}
              labels={{ RECEITA: "Entrada", DESPESA: "Saída" }}
              onChange={(value) => {
                field.onChange(value);
                setValue("categoriaId", undefined as unknown as number);
              }}
            />
          )}
        />
      </Field>
      <Field label="Descrição" htmlFor="mov-descricao" error={errors.descricao?.message}>
        <Input id="mov-descricao" placeholder="Ex.: Café na padaria" aria-invalid={!!errors.descricao} {...register("descricao")} />
      </Field>
      <div className="grid gap-4 sm:grid-cols-2">
        <Field label="Valor" htmlFor="mov-valor" error={errors.valor?.message}>
          <Controller
            control={control}
            name="valor"
            render={({ field }) => (
              <MoneyInput id="mov-valor" value={field.value} onChange={field.onChange} aria-invalid={!!errors.valor} />
            )}
          />
        </Field>
        <Field label="Data" htmlFor="mov-data" error={errors.data?.message}>
          <Controller
            control={control}
            name="data"
            render={({ field }) => (
              <DatePicker id="mov-data" value={field.value || undefined} onChange={(v) => field.onChange(v ?? "")} invalid={!!errors.data} />
            )}
          />
        </Field>
      </div>
      <div className="grid gap-4 sm:grid-cols-2">
        <Field label="Conta" htmlFor="mov-conta" error={errors.contaId?.message}>
          <Controller
            control={control}
            name="contaId"
            render={({ field }) => (
              <ContaSelect id="mov-conta" value={field.value} onChange={field.onChange} invalid={!!errors.contaId} />
            )}
          />
        </Field>
        <Field label="Categoria" htmlFor="mov-categoria" error={errors.categoriaId?.message}>
          <Controller
            control={control}
            name="categoriaId"
            render={({ field }) => (
              <CategoriaSelect id="mov-categoria" tipo={tipo} value={field.value} onChange={field.onChange} invalid={!!errors.categoriaId} />
            )}
          />
        </Field>
      </div>
      <DialogFooter className="mt-2">
        <Button type="button" variant="outline" onClick={onDone}>
          Cancelar
        </Button>
        <Button type="submit" disabled={registrar.isPending}>
          {registrar.isPending && <Spinner className="size-4 text-current" />}
          Registrar lançamento
        </Button>
      </DialogFooter>
    </form>
  );
}
