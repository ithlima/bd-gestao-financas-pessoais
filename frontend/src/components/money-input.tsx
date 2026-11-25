"use client";

import { forwardRef } from "react";
import { formatNumber } from "@/lib/format";
import { cn } from "@/lib/utils";

interface MoneyInputProps extends Omit<React.ComponentProps<"input">, "value" | "onChange"> {
  value: number | undefined;
  onChange: (value: number | undefined) => void;
}

export const MoneyInput = forwardRef<HTMLInputElement, MoneyInputProps>(function MoneyInput(
  { value, onChange, className, ...props },
  ref,
) {
  return (
    <div
      className={cn(
        "group flex h-9 w-full items-center rounded-lg border border-input bg-card transition-colors focus-within:border-ring focus-within:ring-3 focus-within:ring-ring/50 has-aria-invalid:border-destructive has-aria-invalid:ring-3 has-aria-invalid:ring-destructive/20",
        className,
      )}
    >
      <span className="pl-3 text-sm font-medium text-muted-foreground">R$</span>
      <input
        ref={ref}
        inputMode="numeric"
        autoComplete="off"
        className="tabular h-full w-full min-w-0 bg-transparent px-2 text-sm outline-none placeholder:text-muted-foreground"
        placeholder="0,00"
        value={value === undefined ? "" : formatNumber(value)}
        onChange={(event) => {
          const digitos = event.target.value.replace(/\D/g, "").slice(0, 15);
          onChange(digitos ? Number(digitos) / 100 : undefined);
        }}
        {...props}
      />
    </div>
  );
});
