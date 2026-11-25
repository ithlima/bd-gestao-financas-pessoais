"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth";
import { Spinner } from "@/components/spinner";

export default function Home() {
  const { status } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (status === "autenticado") router.replace("/painel");
    if (status === "anonimo") router.replace("/entrar");
  }, [status, router]);

  return (
    <div className="grid min-h-screen place-items-center">
      <Spinner />
    </div>
  );
}
