"use client";

import { useState } from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ThemeProvider } from "next-themes";
import { SkulletProvider } from "skullet/react";
import { AuthProvider } from "@/lib/auth";
import { ApiError } from "@/lib/api";
import { Toaster } from "@/components/ui/sonner";
import { TooltipProvider } from "@/components/ui/tooltip";

export function Providers({ children }: { children: React.ReactNode }) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            staleTime: 15_000,
            refetchOnWindowFocus: false,
            retry: (count, error) => !(error instanceof ApiError && error.status >= 400 && error.status < 500) && count < 1,
          },
        },
      }),
  );

  return (
    <ThemeProvider attribute="class" defaultTheme="light" enableSystem disableTransitionOnChange>
      <QueryClientProvider client={queryClient}>
        <AuthProvider>
          <TooltipProvider delayDuration={200}>
            <SkulletProvider
              mode="light"
              reveal="smart"
              theme={{
                light: { base: "var(--skeleton)", highlight: "var(--skeleton-highlight)" },
                radius: "6px",
                animation: "shimmer",
                speed: "slow",
              }}
            >
              {children}
            </SkulletProvider>
            <Toaster position="top-right" richColors={false} closeButton />
          </TooltipProvider>
        </AuthProvider>
      </QueryClientProvider>
    </ThemeProvider>
  );
}
