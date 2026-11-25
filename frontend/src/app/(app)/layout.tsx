"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { AppShell } from "@/components/app-shell";
import { BrandMark } from "@/components/brand";
import { useAuth } from "@/lib/auth";

export default function AppLayout({ children }: { children: React.ReactNode }) {
  const { status } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (status === "anonimo") router.replace("/entrar");
  }, [status, router]);

  if (status !== "autenticado") {
    return (
      <div className="grid min-h-screen place-items-center">
        <BrandMark className="size-10 animate-pulse" />
      </div>
    );
  }

  return <AppShell>{children}</AppShell>;
}
