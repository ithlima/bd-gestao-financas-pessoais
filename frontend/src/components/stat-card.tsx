"use client";

import { usePathname } from "next/navigation";
import { Skeleton } from "skullet/react";
import { cn } from "@/lib/utils";

interface StatCardProps {
  label: string;
  value: React.ReactNode;
  hint?: React.ReactNode;
  loading?: boolean;
  className?: string;
}

export function StatCard({ label, value, hint, loading, className }: StatCardProps) {
  const pathname = usePathname();
  return (
    <div className={cn("rounded-2xl border bg-card p-5", className)}>
      <p className="text-[13px] font-medium text-muted-foreground">{label}</p>
      <Skeleton id={`${pathname}:${label}`} loading={!!loading} label={`Carregando ${label.toLowerCase()}`}>
        <div className="mt-3 text-2xl font-semibold tracking-tight">{value}</div>
        {hint && <div className="mt-1.5 text-xs text-muted-foreground">{hint}</div>}
      </Skeleton>
    </div>
  );
}
