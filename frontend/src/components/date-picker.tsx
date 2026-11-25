"use client";

import { useState } from "react";
import { parseISO } from "date-fns";
import { ptBR } from "react-day-picker/locale";
import { CalendarDays, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Calendar } from "@/components/ui/calendar";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { formatDate, toIsoDate } from "@/lib/format";
import { cn } from "@/lib/utils";

interface DatePickerProps {
  value?: string;
  onChange: (value: string | undefined) => void;
  placeholder?: string;
  clearable?: boolean;
  className?: string;
  invalid?: boolean;
  id?: string;
}

export function DatePicker({
  value,
  onChange,
  placeholder = "Selecione a data",
  clearable,
  className,
  invalid,
  id,
}: DatePickerProps) {
  const [open, setOpen] = useState(false);
  const selected = value ? parseISO(value) : undefined;

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button
          id={id}
          type="button"
          variant="outline"
          aria-invalid={invalid || undefined}
          className={cn(
            "h-9 w-full justify-start gap-2 bg-card px-3 font-normal",
            !value && "text-muted-foreground",
            className,
          )}
        >
          <CalendarDays className="text-muted-foreground" />
          <span className="flex-1 truncate text-left" title={value ? formatDate(value, "dd 'de' MMM, yyyy") : placeholder}>{value ? formatDate(value, "dd 'de' MMM, yyyy") : placeholder}</span>
          {clearable && value && (
            <span
              role="button"
              tabIndex={-1}
              aria-label="Limpar data"
              onClick={(event) => {
                event.stopPropagation();
                onChange(undefined);
              }}
              className="-mr-1 grid size-5 place-items-center rounded text-muted-foreground hover:bg-muted hover:text-foreground"
            >
              <X className="size-3.5" />
            </span>
          )}
        </Button>
      </PopoverTrigger>
      <PopoverContent className="w-auto p-0" align="start">
        <Calendar
          mode="single"
          locale={ptBR}
          selected={selected}
          defaultMonth={selected}
          captionLayout="dropdown"
          onSelect={(date) => {
            if (date) onChange(toIsoDate(date));
            setOpen(false);
          }}
        />
      </PopoverContent>
    </Popover>
  );
}
