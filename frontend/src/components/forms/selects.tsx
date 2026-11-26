"use client";

import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { formatMoney } from "@/lib/format";
import { useCategorias, useContas } from "@/lib/queries";
import type { TipoMovimentacao } from "@/lib/types";

interface BaseSelectProps {
  value: number | undefined;
  onChange: (value: number | undefined) => void;
  invalid?: boolean;
  id?: string;
  placeholder?: string;
}

export function ContaSelect({
  value,
  onChange,
  invalid,
  id,
  placeholder = "Selecione a conta",
  excluir,
}: BaseSelectProps & { excluir?: number }) {
  const { data: contas = [], isLoading } = useContas();
  const opcoes = contas.filter((conta) => conta.idConta !== excluir);

  return (
    <Select value={value ? String(value) : ""} onValueChange={(v) => onChange(v ? Number(v) : undefined)}>
      <SelectTrigger id={id} className="w-full min-w-0 [&_[data-saldo]]:hidden" aria-invalid={invalid || undefined}>
        <SelectValue placeholder={isLoading ? "Carregando..." : placeholder} />
      </SelectTrigger>
      <SelectContent position="popper">
        {opcoes.length === 0 && (
          <div className="px-2 py-3 text-center text-xs text-muted-foreground">Nenhuma conta disponível</div>
        )}
        {opcoes.map((conta) => (
          <SelectItem key={conta.idConta} value={String(conta.idConta)}>
            <span className="flex w-full items-center justify-between gap-6">
              <span>{conta.nome}</span>
              <span data-saldo className="tabular text-xs text-muted-foreground">{formatMoney(conta.saldoAtual)}</span>
            </span>
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}

export function CategoriaSelect({
  value,
  onChange,
  invalid,
  id,
  tipo,
  placeholder = "Selecione a categoria",
}: BaseSelectProps & { tipo: TipoMovimentacao | undefined }) {
  const { data: categorias = [], isLoading } = useCategorias(tipo);

  return (
    <Select
      value={value ? String(value) : ""}
      onValueChange={(v) => onChange(v ? Number(v) : undefined)}
      disabled={!tipo}
    >
      <SelectTrigger id={id} className="w-full" aria-invalid={invalid || undefined}>
        <SelectValue placeholder={isLoading ? "Carregando..." : placeholder} />
      </SelectTrigger>
      <SelectContent position="popper">
        {categorias.length === 0 && (
          <div className="px-2 py-3 text-center text-xs text-muted-foreground">
            Nenhuma categoria de {tipo === "RECEITA" ? "receita" : "despesa"} cadastrada
          </div>
        )}
        {categorias.map((categoria) => (
          <SelectItem key={categoria.idCategoria} value={String(categoria.idCategoria)}>
            {categoria.nome}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}
