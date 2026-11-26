"use client";

import { RefreshCw, ServerCrash } from "lucide-react";
import { ApiError } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { EmptyState } from "@/components/empty-state";

export function ErrorState({ error, onRetry }: { error: unknown; onRetry?: () => void }) {
  const mensagem =
    error instanceof ApiError ? error.message : "Não foi possível carregar as informações.";
  return (
    <EmptyState
      icon={ServerCrash}
      title="Algo deu errado"
      description={mensagem}
      action={
        onRetry && (
          <Button variant="outline" onClick={onRetry}>
            <RefreshCw />
            Tentar novamente
          </Button>
        )
      }
    />
  );
}
