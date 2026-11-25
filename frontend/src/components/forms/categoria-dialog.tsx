"use client";

import { Controller, useForm } from "react-hook-form";
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
import { Spinner } from "@/components/spinner";
import { TipoToggle } from "@/components/tipo-toggle";
import { useUsuarioId } from "@/lib/auth";
import { useSalvarCategoria } from "@/lib/queries";
import type { Categoria, TipoMovimentacao } from "@/lib/types";

const schema = z.object({
  nome: z
    .string()
    .trim()
    .min(1, "O nome da categoria é obrigatório")
    .max(100, "O nome da categoria deve ter no máximo 100 caracteres"),
  tipo: z.enum(["RECEITA", "DESPESA"], { message: "O tipo da categoria é obrigatório" }),
});

type FormData = z.infer<typeof schema>;

interface CategoriaDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  categoria?: Categoria | null;
  tipoInicial?: TipoMovimentacao;
}

export function CategoriaDialog({ open, onOpenChange, categoria, tipoInicial }: CategoriaDialogProps) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>{categoria ? "Editar categoria" : "Nova categoria"}</DialogTitle>
          <DialogDescription>Categorias classificam títulos e movimentações e formam as linhas dos Resultados.</DialogDescription>
        </DialogHeader>
        <CategoriaForm categoria={categoria} tipoInicial={tipoInicial} onDone={() => onOpenChange(false)} />
      </DialogContent>
    </Dialog>
  );
}

function CategoriaForm({
  categoria,
  tipoInicial,
  onDone,
}: {
  categoria?: Categoria | null;
  tipoInicial?: TipoMovimentacao;
  onDone: () => void;
}) {
  const usuarioId = useUsuarioId();
  const salvar = useSalvarCategoria();
  const {
    register,
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: {
      nome: categoria?.nome ?? "",
      tipo: categoria?.tipo ?? tipoInicial ?? "DESPESA",
    },
  });

  const onSubmit = handleSubmit((data) => {
    salvar.mutate({ id: categoria?.idCategoria, data: { ...data, usuarioId } }, { onSuccess: onDone });
  });

  return (
    <form onSubmit={onSubmit} className="grid gap-4" noValidate>
      <Field label="Tipo" error={errors.tipo?.message}>
        <Controller
          control={control}
          name="tipo"
          render={({ field }) => <TipoToggle value={field.value} onChange={field.onChange} />}
        />
      </Field>
      {categoria && (
        <p className="-mt-2 rounded-lg bg-alerta-soft px-3 py-2 text-xs text-alerta">
          Alterar o tipo de uma categoria já usada pode deixá-la incoerente com os lançamentos existentes.
        </p>
      )}
      <Field label="Nome" htmlFor="categoria-nome" error={errors.nome?.message}>
        <Input id="categoria-nome" placeholder="Ex.: Moradia" aria-invalid={!!errors.nome} {...register("nome")} />
      </Field>
      <DialogFooter className="mt-2">
        <Button type="button" variant="outline" onClick={onDone}>
          Cancelar
        </Button>
        <Button type="submit" disabled={salvar.isPending}>
          {salvar.isPending && <Spinner className="size-4 text-current" />}
          {categoria ? "Salvar alterações" : "Criar categoria"}
        </Button>
      </DialogFooter>
    </form>
  );
}
