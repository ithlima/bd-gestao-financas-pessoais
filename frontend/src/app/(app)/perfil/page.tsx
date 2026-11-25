"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Controller, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { CalendarDays, Mail, ShieldCheck, Trash } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Switch } from "@/components/ui/switch";
import { ConfirmDialog } from "@/components/confirm-dialog";
import { Field } from "@/components/field";
import { PageHeader } from "@/components/page-header";
import { Spinner } from "@/components/spinner";
import { Pill } from "@/components/status-badge";
import { useAuth } from "@/lib/auth";
import { formatDate, initials } from "@/lib/format";
import { useAtualizarUsuario, useRemoverUsuario } from "@/lib/queries";

const schema = z.object({
  nome: z.string().trim().min(1, "O nome é obrigatório").max(100, "O nome deve ter no máximo 100 caracteres"),
  email: z
    .string()
    .trim()
    .min(1, "O e-mail é obrigatório")
    .email("O e-mail deve ser válido")
    .max(150, "O e-mail deve ter no máximo 150 caracteres"),
  senha: z.string().min(6, "A senha deve ter entre 6 e 255 caracteres").max(255, "A senha deve ter entre 6 e 255 caracteres"),
  ativo: z.boolean(),
});

type FormData = z.infer<typeof schema>;

export default function PerfilPage() {
  const { usuario, atualizarUsuario, sair } = useAuth();
  const router = useRouter();
  const atualizar = useAtualizarUsuario();
  const remover = useRemoverUsuario();
  const [excluindo, setExcluindo] = useState(false);
  const [confirmandoInativacao, setConfirmandoInativacao] = useState<FormData | null>(null);

  const {
    register,
    control,
    handleSubmit,
    reset,
    formState: { errors, isDirty },
  } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: { nome: usuario?.nome ?? "", email: usuario?.email ?? "", senha: "", ativo: usuario?.ativo ?? true },
  });

  if (!usuario) return null;

  const enviar = (data: FormData) => {
    atualizar.mutate(
      { id: usuario.idUsuario, data },
      {
        onSuccess: (atualizado) => {
          setConfirmandoInativacao(null);
          if (!atualizado.ativo) {
            toast.info("Conta desativada", { description: "Sua sessão foi encerrada." });
            sair();
            router.replace("/entrar");
            return;
          }
          atualizarUsuario(atualizado);
          reset({ nome: atualizado.nome, email: atualizado.email, senha: "", ativo: atualizado.ativo });
        },
      },
    );
  };

  const onSubmit = handleSubmit((data) => {
    if (!data.ativo) {
      setConfirmandoInativacao(data);
      return;
    }
    enviar(data);
  });

  return (
    <div className="mx-auto grid w-full max-w-2xl gap-8">
      <PageHeader title="Meu perfil" />

      <div className="grid gap-6">
        <section className="overflow-hidden rounded-2xl border bg-card">
          <div className="h-24 border-b bg-muted/60" />
          <div className="relative -mt-10 px-6 pb-6">
            <div className="grid size-20 place-items-center rounded-2xl bg-brand text-xl font-semibold text-brand-foreground ring-4 ring-card">
              {initials(usuario.nome)}
            </div>
            <p className="mt-4 text-lg font-semibold tracking-tight">{usuario.nome}</p>
            <div className="mt-1">
              <Pill tone={usuario.ativo ? "receita" : "neutral"}>{usuario.ativo ? "Conta ativa" : "Conta inativa"}</Pill>
            </div>
            <dl className="mt-6 grid gap-3 text-sm">
              <div className="flex items-center gap-3">
                <Mail className="size-4 shrink-0 text-muted-foreground" />
                <dd className="min-w-0 truncate" title={usuario.email}>{usuario.email}</dd>
              </div>
              <div className="flex items-center gap-3">
                <CalendarDays className="size-4 text-muted-foreground" />
                <dd>Cadastro em {formatDate(usuario.dataCadastro.slice(0, 10), "dd 'de' MMMM 'de' yyyy")}</dd>
              </div>
              <div className="flex items-center gap-3">
                <ShieldCheck className="size-4 text-muted-foreground" />
                <dd>Identificador #{usuario.idUsuario}</dd>
              </div>
            </dl>
          </div>
        </section>

        <div className="grid gap-6">
          <form onSubmit={onSubmit} className="rounded-2xl border bg-card" noValidate>
            <div className="border-b px-6 py-5">
              <p className="font-medium">Dados da conta</p>
              <p className="text-sm text-muted-foreground">Para salvar alterações, informe a senha (atual ou nova).</p>
            </div>
            <div className="grid gap-5 p-6 sm:grid-cols-2">
              <Field label="Nome" htmlFor="perfil-nome" error={errors.nome?.message}>
                <Input id="perfil-nome" aria-invalid={!!errors.nome} {...register("nome")} />
              </Field>
              <Field label="E-mail" htmlFor="perfil-email" error={errors.email?.message}>
                <Input id="perfil-email" type="email" aria-invalid={!!errors.email} {...register("email")} />
              </Field>
              <Field label="Senha" htmlFor="perfil-senha" error={errors.senha?.message} hint="Mínimo de 6 caracteres">
                <Input id="perfil-senha" type="password" autoComplete="new-password" placeholder="••••••" aria-invalid={!!errors.senha} {...register("senha")} />
              </Field>
              <Field label="Situação" htmlFor="perfil-ativo">
                <Controller
                  control={control}
                  name="ativo"
                  render={({ field }) => (
                    <label htmlFor="perfil-ativo" className="flex h-9 items-center justify-between rounded-lg border bg-card px-3 text-sm">
                      <span>{field.value ? "Conta ativa" : "Conta inativa"}</span>
                      <Switch id="perfil-ativo" checked={field.value} onCheckedChange={field.onChange} />
                    </label>
                  )}
                />
              </Field>
            </div>
            <div className="flex justify-end gap-2 border-t px-6 py-4">
              <Button
                type="button"
                variant="outline"
                disabled={!isDirty || atualizar.isPending}
                onClick={() => reset({ nome: usuario.nome, email: usuario.email, senha: "", ativo: usuario.ativo })}
              >
                Descartar
              </Button>
              <Button type="submit" disabled={atualizar.isPending}>
                {atualizar.isPending && <Spinner className="size-4 text-current" />}
                Salvar alterações
              </Button>
            </div>
          </form>

          <section className="rounded-2xl border border-destructive/20 bg-card">
            <div className="flex flex-col gap-4 p-6 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <p className="font-medium">Excluir conta</p>
                <p className="mt-1 max-w-lg text-sm text-muted-foreground">
                  Só é possível excluir o usuário quando não houver títulos, movimentações, contas ou categorias vinculados.
                </p>
              </div>
              <Button variant="destructive" onClick={() => setExcluindo(true)}>
                <Trash />
                Excluir usuário
              </Button>
            </div>
          </section>
        </div>
      </div>

      <ConfirmDialog
        open={!!confirmandoInativacao}
        onOpenChange={(open) => !open && setConfirmandoInativacao(null)}
        title="Desativar conta?"
        description="Usuários inativos não conseguem acessar o sistema. Sua sessão será encerrada após salvar."
        confirmLabel="Desativar e salvar"
        destructive
        loading={atualizar.isPending}
        onConfirm={() => confirmandoInativacao && enviar(confirmandoInativacao)}
      />
      <ConfirmDialog
        open={excluindo}
        onOpenChange={setExcluindo}
        title="Excluir usuário?"
        description="Esta ação remove definitivamente seu cadastro e não pode ser desfeita."
        confirmLabel="Excluir definitivamente"
        destructive
        loading={remover.isPending}
        onConfirm={() =>
          remover.mutate(usuario.idUsuario, {
            onSuccess: () => {
              toast.success("Usuário excluído");
              sair();
              router.replace("/entrar");
            },
            onSettled: () => setExcluindo(false),
          })
        }
      />
    </div>
  );
}
