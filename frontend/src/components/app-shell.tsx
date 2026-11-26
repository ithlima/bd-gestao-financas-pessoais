"use client";

import { useEffect, useState, useSyncExternalStore } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useTheme } from "next-themes";
import {
  ArrowDownToLine,
  ArrowLeftRight,
  ArrowUpFromLine,
  CalendarClock,
  ChartColumnBig,
  ChevronsUpDown,
  LayoutDashboard,
  LogOut,
  Menu,
  Monitor,
  Moon,
  ChevronLeft,
  Sun,
  Tags,
  UserRound,
  Wallet,
  type LucideIcon,
} from "lucide-react";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuRadioGroup,
  DropdownMenuRadioItem,
  DropdownMenuSeparator,
  DropdownMenuSub,
  DropdownMenuSubContent,
  DropdownMenuSubTrigger,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Sheet, SheetContent, SheetTitle, SheetTrigger } from "@/components/ui/sheet";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { Brand, BrandMark } from "@/components/brand";
import { useAuth } from "@/lib/auth";
import { initials } from "@/lib/format";
import { cn } from "@/lib/utils";

interface NavItem {
  href: string;
  label: string;
  icon: LucideIcon;
}

const navegacao: { titulo: string; itens: NavItem[] }[] = [
  {
    titulo: "Visão geral",
    itens: [{ href: "/painel", label: "Painel", icon: LayoutDashboard }],
  },
  {
    titulo: "Caixa",
    itens: [
      { href: "/contas", label: "Contas", icon: Wallet },
      { href: "/movimentacoes", label: "Movimentações", icon: ArrowLeftRight },
    ],
  },
  {
    titulo: "Planejamento",
    itens: [
      { href: "/titulos", label: "Títulos", icon: CalendarClock },
      { href: "/contas-a-pagar", label: "Contas a pagar", icon: ArrowUpFromLine },
      { href: "/contas-a-receber", label: "Contas a receber", icon: ArrowDownToLine },
    ],
  },
  {
    titulo: "Análise",
    itens: [{ href: "/dre", label: "Resultados", icon: ChartColumnBig }],
  },
  {
    titulo: "Cadastros",
    itens: [{ href: "/categorias", label: "Categorias", icon: Tags }],
  },
];

const chaveRecolhida = "financas.sidebar-recolhida";
const ouvintes = new Set<() => void>();

function lerRecolhida() {
  try {
    return localStorage.getItem(chaveRecolhida) === "1";
  } catch {
    return false;
  }
}

function gravarRecolhida(valor: boolean) {
  try {
    localStorage.setItem(chaveRecolhida, valor ? "1" : "0");
  } catch {}
  ouvintes.forEach((ouvinte) => ouvinte());
}

function useSidebarRecolhida() {
  return useSyncExternalStore(
    (ouvinte) => {
      ouvintes.add(ouvinte);
      return () => ouvintes.delete(ouvinte);
    },
    lerRecolhida,
    () => false,
  );
}

function textoRecolhivel(recolhida: boolean) {
  return cn(
    "transition-opacity ease-out motion-reduce:delay-0",
    recolhida ? "opacity-0 duration-100" : "opacity-100 delay-100 duration-200",
  );
}

function NavLinks({ onNavigate, recolhida = false }: { onNavigate?: () => void; recolhida?: boolean }) {
  const pathname = usePathname();
  return (
    <nav className="grid gap-6">
      {navegacao.map((grupo) => (
        <div key={grupo.titulo}>
          <div className="relative mb-2 h-4">
            <p
              className={cn(
                "absolute inset-x-3 top-0 truncate text-[10.5px] font-semibold tracking-[0.14em] text-sidebar-foreground/45 uppercase",
                textoRecolhivel(recolhida),
              )}
              aria-hidden={recolhida}
            >
              {grupo.titulo}
            </p>
            <span
              className={cn(
                "absolute inset-x-3 top-2 h-px bg-sidebar-border transition-opacity ease-out",
                recolhida ? "opacity-100 delay-100 duration-200" : "opacity-0 duration-100",
              )}
            />
          </div>
          <div className="grid gap-0.5">
            {grupo.itens.map((item) => {
              const ativo = pathname === item.href || pathname.startsWith(`${item.href}/`);
              const link = (
                <Link
                  href={item.href}
                  onClick={onNavigate}
                  aria-label={recolhida ? item.label : undefined}
                  className={cn(
                    "group relative flex h-9 items-center gap-3 overflow-hidden rounded-lg px-3 text-[13.5px] font-medium whitespace-nowrap transition-colors",
                    ativo
                      ? "bg-sidebar-accent text-sidebar-accent-foreground"
                      : "text-sidebar-foreground hover:bg-sidebar-accent/60 hover:text-sidebar-accent-foreground",
                  )}
                >
                  {ativo && <span className="absolute top-2 bottom-2 left-0 w-[3px] rounded-full bg-sidebar-primary" />}
                  <item.icon
                    className={cn(
                      "size-4 shrink-0 transition-colors",
                      ativo ? "text-sidebar-primary" : "text-sidebar-foreground/60 group-hover:text-sidebar-accent-foreground",
                    )}
                  />
                  <span className={textoRecolhivel(recolhida)}>{item.label}</span>
                </Link>
              );
              return (
                <Tooltip key={item.href}>
                  <TooltipTrigger asChild>{link}</TooltipTrigger>
                  {recolhida && (
                    <TooltipContent side="right" sideOffset={8}>
                      {item.label}
                    </TooltipContent>
                  )}
                </Tooltip>
              );
            })}
          </div>
        </div>
      ))}
    </nav>
  );
}

function TemaSubmenu() {
  const { theme, setTheme } = useTheme();
  const opcoes = [
    { valor: "light", label: "Claro", icon: Sun },
    { valor: "dark", label: "Escuro", icon: Moon },
    { valor: "system", label: "Sistema", icon: Monitor },
  ];
  const atual = opcoes.find((o) => o.valor === theme) ?? opcoes[0];
  return (
    <DropdownMenuSub>
      <DropdownMenuSubTrigger>
        <atual.icon />
        Tema
      </DropdownMenuSubTrigger>
      <DropdownMenuSubContent>
        <DropdownMenuRadioGroup value={theme ?? "light"} onValueChange={setTheme}>
          {opcoes.map((opcao) => (
            <DropdownMenuRadioItem key={opcao.valor} value={opcao.valor}>
              <opcao.icon />
              {opcao.label}
            </DropdownMenuRadioItem>
          ))}
        </DropdownMenuRadioGroup>
      </DropdownMenuSubContent>
    </DropdownMenuSub>
  );
}

function UserMenu({ compact, recolhida }: { compact?: boolean; recolhida?: boolean }) {
  const { usuario, sair } = useAuth();
  const router = useRouter();
  const somenteAvatar = compact || recolhida;

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <button
          aria-label={somenteAvatar ? "Menu da conta" : undefined}
          className={cn(
            "flex items-center gap-3 overflow-hidden rounded-xl text-left outline-none",
            compact
              ? "p-0"
              : "w-full border p-1.5 pr-2.5 transition-[background-color,border-color] duration-200 ease-out hover:bg-sidebar-accent",
            !compact && (recolhida ? "border-transparent bg-transparent" : "border-sidebar-border bg-sidebar-accent/40"),
          )}
        >
          <Avatar className="size-8 shrink-0 rounded-lg">
            <AvatarFallback className="rounded-lg bg-brand text-xs font-semibold text-brand-foreground">
              {initials(usuario?.nome)}
            </AvatarFallback>
          </Avatar>
          {!compact && (
            <>
              <div className={cn("min-w-0 flex-1", textoRecolhivel(!!recolhida))}>
                <p className="truncate text-[13px] font-medium text-sidebar-accent-foreground" title={usuario?.nome}>{usuario?.nome}</p>
                <p className="truncate text-[11.5px] text-sidebar-foreground/60" title={usuario?.email}>{usuario?.email}</p>
              </div>
              <ChevronsUpDown className={cn("size-4 shrink-0 text-sidebar-foreground/50", textoRecolhivel(!!recolhida))} />
            </>
          )}
        </button>
      </DropdownMenuTrigger>
      <DropdownMenuContent
        align={compact ? "end" : "start"}
        side={compact ? "bottom" : recolhida ? "right" : "top"}
        sideOffset={recolhida ? 12 : 4}
        className="w-56"
      >
        <DropdownMenuLabel className="font-normal">
          <p className="text-sm font-medium text-foreground [overflow-wrap:anywhere]">{usuario?.nome}</p>
          <p className="truncate text-xs text-muted-foreground" title={usuario?.email}>{usuario?.email}</p>
        </DropdownMenuLabel>
        <DropdownMenuSeparator />
        <DropdownMenuItem onSelect={() => router.push("/perfil")}>
          <UserRound />
          Meu perfil
        </DropdownMenuItem>
        <TemaSubmenu />
        <DropdownMenuSeparator />
        <DropdownMenuItem
          variant="destructive"
          onSelect={() => {
            sair();
            router.replace("/entrar");
          }}
        >
          <LogOut />
          Sair
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}

export function AppShell({ children }: { children: React.ReactNode }) {
  const [menuAberto, setMenuAberto] = useState(false);
  const recolhida = useSidebarRecolhida();
  const [animar, setAnimar] = useState(false);

  useEffect(() => {
    const quadro = requestAnimationFrame(() => setAnimar(true));
    return () => cancelAnimationFrame(quadro);
  }, []);

  const movimento = animar && "duration-250 ease-in-out motion-reduce:transition-none";

  return (
    <div
      className={cn(
        "min-h-screen transition-[padding]",
        movimento,
        recolhida ? "lg:pl-[68px]" : "lg:pl-64",
      )}
    >
      <aside
        className={cn(
          "fixed inset-y-0 left-0 z-30 hidden flex-col border-r border-sidebar-border bg-sidebar transition-[width] lg:flex",
          movimento,
          recolhida ? "w-[68px]" : "w-64",
        )}
      >
        <div className="flex h-16 items-center gap-2.5 overflow-hidden px-[17px]">
          <BrandMark />
          <p className={cn("text-[15px] leading-none font-semibold tracking-tight whitespace-nowrap", textoRecolhivel(recolhida))}>
            Fluxo
          </p>
        </div>
        <Tooltip>
          <TooltipTrigger asChild>
            <button
              type="button"
              onClick={() => gravarRecolhida(!recolhida)}
              aria-label={recolhida ? "Expandir menu" : "Recolher menu"}
              aria-expanded={!recolhida}
              className="absolute top-5 -right-3 z-10 grid size-6 place-items-center rounded-full border bg-background text-muted-foreground shadow-sm transition-[color,transform] duration-150 ease-out outline-none hover:text-foreground focus-visible:ring-3 focus-visible:ring-ring/50 active:scale-[0.94]"
            >
              <ChevronLeft
                className={cn(
                  "size-3.5 transition-transform duration-250 ease-in-out motion-reduce:transition-none",
                  recolhida && "rotate-180",
                )}
              />
            </button>
          </TooltipTrigger>
          <TooltipContent side="right" sideOffset={8}>
            {recolhida ? "Expandir menu" : "Recolher menu"}
          </TooltipContent>
        </Tooltip>
        <div className="flex-1 overflow-x-hidden overflow-y-auto px-3 py-4">
          <NavLinks recolhida={recolhida} />
        </div>
        <div className="p-3">
          <UserMenu recolhida={recolhida} />
        </div>
      </aside>

      <header className="sticky top-0 z-20 flex h-14 items-center justify-between border-b bg-background/80 px-4 backdrop-blur-md lg:hidden">
        <Sheet open={menuAberto} onOpenChange={setMenuAberto}>
          <SheetTrigger asChild>
            <Button variant="ghost" size="icon" aria-label="Abrir menu">
              <Menu />
            </Button>
          </SheetTrigger>
          <SheetContent side="left" className="w-72 border-sidebar-border bg-sidebar p-0 text-sidebar-foreground">
            <SheetTitle className="sr-only">Navegação</SheetTitle>
            <div className="flex h-16 items-center px-5">
              <Brand />
            </div>
            <div className="px-3 py-2">
              <NavLinks onNavigate={() => setMenuAberto(false)} />
            </div>
          </SheetContent>
        </Sheet>
        <Brand />
        <UserMenu compact />
      </header>

      <main className="mx-auto w-full max-w-[1400px] px-4 py-6 sm:px-6 lg:px-10 lg:py-10">{children}</main>
    </div>
  );
}
