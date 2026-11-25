"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Field } from "@/components/field";
import { Spinner } from "@/components/spinner";
import { useAuth } from "@/lib/auth";
import { notifyError } from "@/lib/queries";

const schema = z.object({
  email: z.string().trim().min(1, "O e-mail é obrigatório").email("O e-mail deve ser válido"),
  senha: z.string().min(1, "A senha é obrigatória"),
});

type FormData = z.infer<typeof schema>;

export default function EntrarPage() {
  const { entrar } = useAuth();
  const router = useRouter();
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({ resolver: zodResolver(schema) });

  const onSubmit = handleSubmit(async ({ email, senha }) => {
    try {
      await entrar(email, senha);
      router.replace("/painel");
    } catch (error) {
      notifyError(error);
    }
  });

  return (
    <div>
      <h1 className="text-2xl font-semibold tracking-tight">Bem-vindo de volta</h1>

      <form onSubmit={onSubmit} className="mt-8 grid gap-4" noValidate>
        <Field label="E-mail" htmlFor="email" error={errors.email?.message}>
          <Input id="email" type="email" autoComplete="email" placeholder="voce@exemplo.com" aria-invalid={!!errors.email} {...register("email")} />
        </Field>
        <Field label="Senha" htmlFor="senha" error={errors.senha?.message}>
          <Input id="senha" type="password" autoComplete="current-password" aria-invalid={!!errors.senha} {...register("senha")} />
        </Field>
        <Button type="submit" size="lg" className="mt-2 w-full" disabled={isSubmitting}>
          {isSubmitting ? <Spinner className="size-4 text-current" /> : null}
          Entrar
        </Button>
      </form>

      <p className="mt-8 text-center text-sm text-muted-foreground">
        Ainda não tem conta?{" "}
        <Link href="/cadastro" className="font-medium text-foreground underline-offset-4 hover:underline">
          Criar cadastro
        </Link>
      </p>
    </div>
  );
}
