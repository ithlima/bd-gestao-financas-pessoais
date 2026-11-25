import { cn } from "@/lib/utils";

export function BrandMark({ className }: { className?: string }) {
  return (
    <span
      className={cn(
        "relative grid size-8 shrink-0 place-items-center overflow-hidden rounded-[10px] bg-brand text-brand-foreground",
        className,
      )}
    >
      <svg viewBox="0 0 24 24" className="size-[60%]" fill="none" aria-hidden>
        <path
          d="M4 16.5c2.4 0 3.2-9 6-9s3 6 5.6 6c1.6 0 2.6-2.2 4.4-5"
          stroke="currentColor"
          strokeWidth="2.6"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      </svg>
    </span>
  );
}

export function Brand({ className, inverted }: { className?: string; inverted?: boolean }) {
  return (
    <div className={cn("flex items-center gap-2.5", className)}>
      <BrandMark />
      <p className={cn("text-[15px] leading-none font-semibold tracking-tight whitespace-nowrap", inverted && "text-white")}>
        Fluxo
      </p>
    </div>
  );
}
