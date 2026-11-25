"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Field } from "@/components/field";
import { Spinner } from "@/components/spinner";
import { useAuth } from "@/lib/auth";
import { notifyError, useCadastrarUsuario } from "@/lib/queries";

const schema = z.object({
  nome: z.string().trim().min(1, "O nome é obrigatório").max(100, "O nome deve ter no máximo 100 caracteres"),
  email: z
    .string()
    .trim()
    .min(1, "O e-mail é obrigatório")
    .email("O e-mail deve ser válido")
    .max(150, "O e-mail deve ter no máximo 150 caracteres"),
  senha: z.string().min(6, "A senha deve ter entre 6 e 255 caracteres").max(255, "A senha deve ter entre 6 e 255 caracteres"),
});

type FormData = z.infer<typeof schema>;

export default function CadastroPage() {
  const { entrar } = useAuth();
  const router = useRouter();
  const cadastrar = useCadastrarUsuario();
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({ resolver: zodResolver(schema) });

  const onSubmit = handleSubmit(async (data) => {
    try {
      await cadastrar.mutateAsync(data);
    } catch {
      return;
    }
    try {
      await entrar(data.email, data.senha);
      toast.success("Cadastro realizado");
      router.replace("/painel");
    } catch (error) {
      notifyError(error);
      router.replace("/entrar");
    }
  });

  return (
    <div>
      <h1 className="text-2xl font-semibold tracking-tight">Crie sua conta</h1>

      <form onSubmit={onSubmit} className="mt-8 grid gap-4" noValidate>
        <Field label="Nome" htmlFor="nome" error={errors.nome?.message}>
          <Input id="nome" autoComplete="name" aria-invalid={!!errors.nome} {...register("nome")} />
        </Field>
        <Field label="E-mail" htmlFor="email" error={errors.email?.message}>
          <Input id="email" type="email" autoComplete="email" placeholder="voce@exemplo.com" aria-invalid={!!errors.email} {...register("email")} />
        </Field>
        <Field label="Senha" htmlFor="senha" error={errors.senha?.message} hint="Mínimo de 6 caracteres">
          <Input id="senha" type="password" autoComplete="new-password" aria-invalid={!!errors.senha} {...register("senha")} />
        </Field>
        <Button type="submit" size="lg" className="mt-2 w-full" disabled={isSubmitting}>
          {isSubmitting ? <Spinner className="size-4 text-current" /> : null}
          Criar conta
        </Button>
      </form>

      <p className="mt-8 text-center text-sm text-muted-foreground">
        Já possui cadastro?{" "}
        <Link href="/entrar" className="font-medium text-foreground underline-offset-4 hover:underline">
          Entrar
        </Link>
      </p>
    </div>
  );
}
