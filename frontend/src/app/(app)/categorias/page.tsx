"use client";

import { useState } from "react";
import { MoreHorizontal, Pencil, Plus, Tags, Trash } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Skeleton } from "skullet/react";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { ConfirmDialog } from "@/components/confirm-dialog";
import { EmptyState } from "@/components/empty-state";
import { ErrorState } from "@/components/error-state";
import { CategoriaDialog } from "@/components/forms/categoria-dialog";
import { PageHeader } from "@/components/page-header";
import { useCategorias, useRemoverCategoria } from "@/lib/queries";
import { plural } from "@/lib/format";
import type { Categoria, TipoMovimentacao } from "@/lib/types";
import { cn } from "@/lib/utils";

type Filtro = "TODOS" | TipoMovimentacao;

export default function CategoriasPage() {
  const [filtro, setFiltro] = useState<Filtro>("TODOS");
  const { data: categorias = [], isLoading, error, refetch } = useCategorias(filtro === "TODOS" ? undefined : filtro);
  const remover = useRemoverCategoria();
  const [dialogAberto, setDialogAberto] = useState(false);
  const [editando, setEditando] = useState<Categoria | null>(null);
  const [excluindo, setExcluindo] = useState<Categoria | null>(null);

  const grupos: { tipo: TipoMovimentacao; titulo: string; itens: Categoria[] }[] = (
    [
      { tipo: "RECEITA", titulo: "Receitas", itens: categorias.filter((c) => c.tipo === "RECEITA") },
      { tipo: "DESPESA", titulo: "Despesas", itens: categorias.filter((c) => c.tipo === "DESPESA") },
    ] as const
  ).filter((g) => filtro === "TODOS" || g.tipo === filtro);

  const abrirNova = () => {
    setEditando(null);
    setDialogAberto(true);
  };

  return (
    <div className="grid gap-8">
      <PageHeader
        title="Categorias"
        description="Cada categoria forma uma linha dos Resultados."
        actions={
          <Button onClick={abrirNova}>
            <Plus />
            Nova categoria
          </Button>
        }
      />

      <Tabs value={filtro} onValueChange={(v) => setFiltro(v as Filtro)}>
        <TabsList>
          <TabsTrigger value="TODOS" className="px-4">
            Todas
          </TabsTrigger>
          <TabsTrigger value="RECEITA" className="px-4">
            Receitas
          </TabsTrigger>
          <TabsTrigger value="DESPESA" className="px-4">
            Despesas
          </TabsTrigger>
        </TabsList>
      </Tabs>

      {error ? (
        <ErrorState error={error} onRetry={refetch} />
      ) : (
        <Skeleton id="categorias-1" loading={isLoading}>
          {categorias.length === 0 ? (
            <div className="rounded-2xl border bg-card">
              <EmptyState
                icon={Tags}
                title="Nenhuma categoria encontrada"
                description="Crie categorias de receita e despesa para classificar seus lançamentos."
                action={
                  <Button onClick={abrirNova}>
                    <Plus />
                    Nova categoria
                  </Button>
                }
              />
            </div>
          ) : (
            <div className={cn("grid gap-6", grupos.length > 1 && "lg:grid-cols-2")}>
              {grupos.map((grupo) => (
                <section key={grupo.tipo} className="overflow-hidden rounded-2xl border bg-card">
                  <header className="flex items-center justify-between border-b px-5 py-4">
                    <div>
                      <p className="font-medium">{grupo.titulo}</p>
                      <p className="text-xs text-muted-foreground">{plural(grupo.itens.length, "categoria", "categorias")}</p>
                    </div>
                  </header>
                  {grupo.itens.length === 0 ? (
                    <p className="px-5 py-10 text-center text-sm text-muted-foreground">Nenhuma categoria deste tipo.</p>
                  ) : (
                    <ul className="divide-y">
                      {grupo.itens.map((categoria) => (
                        <li
                          key={categoria.idCategoria}
                          className="flex items-center justify-between gap-3 px-5 py-3 transition-colors hover:bg-muted/40"
                        >
                          <span className="min-w-0 truncate text-sm font-medium" title={categoria.nome}>
                            {categoria.nome}
                          </span>
                          <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                              <Button variant="ghost" size="icon-sm" aria-label="Ações da categoria">
                                <MoreHorizontal />
                              </Button>
                            </DropdownMenuTrigger>
                            <DropdownMenuContent align="end" className="w-40">
                              <DropdownMenuItem
                                onSelect={() => {
                                  setEditando(categoria);
                                  setDialogAberto(true);
                                }}
                              >
                                <Pencil />
                                Editar
                              </DropdownMenuItem>
                              <DropdownMenuSeparator />
                              <DropdownMenuItem variant="destructive" onSelect={() => setExcluindo(categoria)}>
                                <Trash />
                                Excluir
                              </DropdownMenuItem>
                            </DropdownMenuContent>
                          </DropdownMenu>
                        </li>
                      ))}
                    </ul>
                  )}
                </section>
              ))}
            </div>
          )}
        </Skeleton>
      )}

      <CategoriaDialog
        open={dialogAberto}
        onOpenChange={setDialogAberto}
        categoria={editando}
        tipoInicial={filtro === "TODOS" ? undefined : filtro}
      />
      <ConfirmDialog
        open={!!excluindo}
        onOpenChange={(open) => !open && setExcluindo(null)}
        title="Excluir categoria?"
        description={
          <>
            A categoria <strong className="text-foreground">{excluindo?.nome}</strong> será removida. Categorias usadas por títulos ou
            movimentações não podem ser excluídas.
          </>
        }
        confirmLabel="Excluir"
        destructive
        loading={remover.isPending}
        onConfirm={() => excluindo && remover.mutate(excluindo.idCategoria, { onSuccess: () => setExcluindo(null) })}
      />
    </div>
  );
}
