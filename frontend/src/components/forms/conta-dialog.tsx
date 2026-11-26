"use client";

import { useState } from "react";
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
import { Field } from "@/components/field";
import { MoneyInput } from "@/components/money-input";
import { Spinner } from "@/components/spinner";
import { useUsuarioId } from "@/lib/auth";
import { tipoContaConhecido, tiposConta } from "@/lib/format";
import { useSalvarConta } from "@/lib/queries";
import type { Conta } from "@/lib/types";
import { cn } from "@/lib/utils";

const schema = z.object({
  nome: z.string().trim().min(1, "O nome da conta é obrigatório").max(100, "O nome da conta deve ter no máximo 100 caracteres"),
  tipo: z.string().trim().min(1, "O tipo da conta é obrigatório").max(50, "O tipo da conta deve ter no máximo 50 caracteres"),
  saldoInicial: z.number().min(0, "O saldo inicial não pode ser negativo").optional(),
});

type FormData = z.infer<typeof schema>;

interface ContaDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  conta?: Conta | null;
}

export function ContaDialog({ open, onOpenChange, conta }: ContaDialogProps) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>{conta ? "Editar conta" : "Nova conta"}</DialogTitle>
          <DialogDescription>
            O saldo atual é calculado a partir do saldo inicial e das movimentações realizadas.
          </DialogDescription>
        </DialogHeader>
        <ContaForm conta={conta} onDone={() => onOpenChange(false)} />
      </DialogContent>
    </Dialog>
  );
}

function ContaForm({ conta, onDone }: { conta?: Conta | null; onDone: () => void }) {
  const usuarioId = useUsuarioId();
  const salvar = useSalvarConta();
  const {
    register,
    control,
    handleSubmit,
    setValue,
    setFocus,
    formState: { errors },
  } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: {
      nome: conta?.nome ?? "",
      tipo: conta?.tipo ?? "CORRENTE",
      saldoInicial: conta?.saldoInicial ?? undefined,
    },
  });
  const [outroTipo, setOutroTipo] = useState(() => !!conta && !tipoContaConhecido(conta.tipo));
  const tipoAtual = useWatch({ control, name: "tipo" });
  const tipoSelecionado = outroTipo ? "OUTRO" : tipoContaConhecido(tipoAtual)?.valor;

  const onSubmit = handleSubmit((data) => {
    salvar.mutate(
      { id: conta?.idConta, data: { ...data, usuarioId } },
      { onSuccess: onDone },
    );
  });

  return (
    <form onSubmit={onSubmit} className="grid gap-4" noValidate>
      <Field label="Nome" htmlFor="conta-nome" error={errors.nome?.message}>
        <Input id="conta-nome" placeholder="Ex.: Banco principal" aria-invalid={!!errors.nome} {...register("nome")} />
      </Field>
      <Field label="Tipo" htmlFor={outroTipo ? "conta-tipo" : undefined} error={errors.tipo?.message}>
        <div role="radiogroup" aria-label="Tipo da conta" className="flex flex-wrap gap-1.5">
          {[...tiposConta, { valor: "OUTRO", label: "Outro" }].map((opcao) => {
            const ativo = tipoSelecionado === opcao.valor;
            return (
              <button
                key={opcao.valor}
                type="button"
                role="radio"
                aria-checked={ativo}
                onClick={() => {
                  if (opcao.valor === "OUTRO") {
                    if (!outroTipo) setValue("tipo", "");
                    setOutroTipo(true);
                    setTimeout(() => setFocus("tipo"), 0);
                    return;
                  }
                  setOutroTipo(false);
                  setValue("tipo", opcao.valor, { shouldValidate: true });
                }}
                className={cn(
                  "h-8 rounded-full border px-3.5 text-[13px] font-medium transition-colors",
                  ativo
                    ? "border-foreground bg-foreground text-background"
                    : "bg-card text-muted-foreground hover:text-foreground",
                )}
              >
                {opcao.label}
              </button>
            );
          })}
        </div>
        {outroTipo && (
          <Input
            id="conta-tipo"
            placeholder="Ex.: Cartão pré-pago"
            maxLength={50}
            aria-invalid={!!errors.tipo}
            className="mt-1"
            {...register("tipo")}
          />
        )}
      </Field>
      <Field label="Saldo inicial" htmlFor="conta-saldo" error={errors.saldoInicial?.message} hint="Opcional. Quando omitido, assume zero.">
        <Controller
          control={control}
          name="saldoInicial"
          render={({ field }) => (
            <MoneyInput id="conta-saldo" value={field.value} onChange={field.onChange} aria-invalid={!!errors.saldoInicial} />
          )}
        />
      </Field>
      <DialogFooter className="mt-2">
        <Button type="button" variant="outline" onClick={onDone}>
          Cancelar
        </Button>
        <Button type="submit" disabled={salvar.isPending}>
          {salvar.isPending && <Spinner className="size-4 text-current" />}
          {conta ? "Salvar alterações" : "Criar conta"}
        </Button>
      </DialogFooter>
    </form>
  );
}
