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
import { Textarea } from "@/components/ui/textarea";
import { DatePicker } from "@/components/date-picker";
import { Field } from "@/components/field";
import { MoneyInput } from "@/components/money-input";
import { Spinner } from "@/components/spinner";
import { TipoToggle } from "@/components/tipo-toggle";
import { CategoriaSelect } from "@/components/forms/selects";
import { useUsuarioId } from "@/lib/auth";
import { formatMoney } from "@/lib/format";
import { useSalvarTitulo } from "@/lib/queries";
import type { TipoMovimentacao, Titulo } from "@/lib/types";

const schema = z.object({
  tipo: z.enum(["RECEITA", "DESPESA"], { message: "O tipo do título é obrigatório" }),
  descricao: z
    .string()
    .trim()
    .min(1, "A descrição do título é obrigatória")
    .max(255, "A descrição deve ter no máximo 255 caracteres"),
  valorPrevisto: z
    .number({ message: "O valor previsto é obrigatório" })
    .positive("O valor previsto deve ser maior que zero"),
  dataVencimento: z.string({ message: "A data de vencimento é obrigatória" }).min(1, "A data de vencimento é obrigatória"),
  categoriaId: z.number({ message: "A categoria é obrigatória" }),
  observacao: z.string().max(255, "A observação deve ter no máximo 255 caracteres").optional(),
});

type FormData = z.infer<typeof schema>;

interface TituloDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  titulo?: Titulo | null;
  tipoInicial?: TipoMovimentacao;
}

export function TituloDialog({ open, onOpenChange, titulo, tipoInicial }: TituloDialogProps) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>{titulo ? "Editar título" : "Novo título"}</DialogTitle>
          <DialogDescription>
            Um título registra um compromisso previsto. Nada acontece no caixa até que ele seja pago ou recebido.
          </DialogDescription>
        </DialogHeader>
        <TituloForm titulo={titulo} tipoInicial={tipoInicial} onDone={() => onOpenChange(false)} />
      </DialogContent>
    </Dialog>
  );
}

function TituloForm({
  titulo,
  tipoInicial,
  onDone,
}: {
  titulo?: Titulo | null;
  tipoInicial?: TipoMovimentacao;
  onDone: () => void;
}) {
  const usuarioId = useUsuarioId();
  const salvar = useSalvarTitulo();
  const {
    register,
    control,
    handleSubmit,
    setValue,
    formState: { errors },
  } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: {
      tipo: titulo?.tipo ?? tipoInicial ?? "DESPESA",
      descricao: titulo?.descricao ?? "",
      valorPrevisto: titulo?.valorPrevisto,
      dataVencimento: titulo?.dataVencimento ?? "",
      categoriaId: titulo?.categoriaId,
      observacao: titulo?.observacao ?? "",
    },
  });
  const tipo = useWatch({ control, name: "tipo" });

  const onSubmit = handleSubmit((data) => {
    salvar.mutate(
      {
        id: titulo?.idTitulo,
        data: { ...data, observacao: data.observacao?.trim() || undefined, usuarioId },
      },
      { onSuccess: onDone },
    );
  });

  return (
    <form onSubmit={onSubmit} className="grid gap-4" noValidate>
      <Field
        label="Tipo"
        error={errors.tipo?.message}
        hint={titulo ? "O tipo não pode ser alterado. Para mudar a direção, cadastre outro título." : undefined}
      >
        <Controller
          control={control}
          name="tipo"
          render={({ field }) => (
            <TipoToggle
              value={field.value}
              disabled={!!titulo}
              labels={{ RECEITA: "A receber", DESPESA: "A pagar" }}
              onChange={(value) => {
                field.onChange(value);
                setValue("categoriaId", undefined as unknown as number);
              }}
            />
          )}
        />
      </Field>
      <Field label="Descrição" htmlFor="titulo-descricao" error={errors.descricao?.message}>
        <Input
          id="titulo-descricao"
          placeholder={tipo === "RECEITA" ? "Ex.: Salário de dezembro" : "Ex.: Aluguel de dezembro"}
          aria-invalid={!!errors.descricao}
          {...register("descricao")}
        />
      </Field>
      <div className="grid gap-4 sm:grid-cols-2">
        <Field
          label="Valor previsto"
          htmlFor="titulo-valor"
          error={errors.valorPrevisto?.message}
          hint={titulo && titulo.valorRealizado > 0 ? `Mínimo: ${formatMoney(titulo.valorRealizado)} já realizado` : undefined}
        >
          <Controller
            control={control}
            name="valorPrevisto"
            render={({ field }) => (
              <MoneyInput id="titulo-valor" value={field.value} onChange={field.onChange} aria-invalid={!!errors.valorPrevisto} />
            )}
          />
        </Field>
        <Field label="Vencimento" htmlFor="titulo-vencimento" error={errors.dataVencimento?.message}>
          <Controller
            control={control}
            name="dataVencimento"
            render={({ field }) => (
              <DatePicker id="titulo-vencimento" value={field.value || undefined} onChange={(v) => field.onChange(v ?? "")} invalid={!!errors.dataVencimento} />
            )}
          />
        </Field>
      </div>
      <Field label="Categoria" htmlFor="titulo-categoria" error={errors.categoriaId?.message}>
        <Controller
          control={control}
          name="categoriaId"
          render={({ field }) => (
            <CategoriaSelect id="titulo-categoria" tipo={tipo} value={field.value} onChange={field.onChange} invalid={!!errors.categoriaId} />
          )}
        />
      </Field>
      <Field label="Observação" htmlFor="titulo-observacao" error={errors.observacao?.message}>
        <Textarea id="titulo-observacao" placeholder="Opcional" className="min-h-20 resize-none" {...register("observacao")} />
      </Field>
      <DialogFooter className="mt-2">
        <Button type="button" variant="outline" onClick={onDone}>
          Cancelar
        </Button>
        <Button type="submit" disabled={salvar.isPending}>
          {salvar.isPending && <Spinner className="size-4 text-current" />}
          {titulo ? "Salvar alterações" : "Cadastrar título"}
        </Button>
      </DialogFooter>
    </form>
  );
}
