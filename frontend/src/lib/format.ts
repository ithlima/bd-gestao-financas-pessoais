import { endOfMonth, format, parseISO, startOfMonth } from "date-fns";
import { ptBR } from "date-fns/locale";

const moeda = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });
const numero = new Intl.NumberFormat("pt-BR", { minimumFractionDigits: 2, maximumFractionDigits: 2 });

export function formatMoney(value: number | null | undefined) {
  return moeda.format(Number(value ?? 0));
}

const inteiro = new Intl.NumberFormat("pt-BR", { maximumFractionDigits: 0 });

export function formatNumber(value: number | null | undefined) {
  return numero.format(Number(value ?? 0));
}

export function formatDate(value: string | null | undefined, pattern = "dd MMM yyyy") {
  if (!value) return "—";
  return format(parseISO(value), pattern, { locale: ptBR });
}

export function toIsoDate(date: Date) {
  return format(date, "yyyy-MM-dd");
}

export function today() {
  return toIsoDate(new Date());
}

export function firstDayOfMonth() {
  return toIsoDate(startOfMonth(new Date()));
}

export function lastDayOfMonth() {
  return toIsoDate(endOfMonth(new Date()));
}

const sufixos = new Set(["jr", "jr.", "junior", "júnior", "sr", "sr.", "filho", "neto", "sobrinho", "ii", "iii", "iv"]);
const segmentador = new Intl.Segmenter("pt-BR", { granularity: "grapheme" });

function primeiroGrafema(palavra: string) {
  return segmentador.segment(palavra)[Symbol.iterator]().next().value?.segment ?? "";
}

export function initials(nome: string | undefined) {
  if (!nome) return "";
  const partes = nome.trim().split(/\s+/).filter((p) => /\p{L}/u.test(p));
  while (partes.length > 1 && sufixos.has(partes[partes.length - 1].toLowerCase())) partes.pop();
  if (partes.length === 0) return "";
  const primeira = primeiroGrafema(partes[0].replace(/^[^\p{L}]+/u, ""));
  const ultima = partes.length > 1 ? primeiroGrafema(partes[partes.length - 1].replace(/^[^\p{L}]+/u, "")) : "";
  return (primeira + ultima).toLocaleUpperCase("pt-BR");
}

export function plural(quantidade: number, singular: string, pluralTexto: string) {
  return `${inteiro.format(quantidade)} ${quantidade === 1 ? singular : pluralTexto}`;
}

export const tiposConta = [
  { valor: "CORRENTE", label: "Conta corrente" },
  { valor: "POUPANCA", label: "Poupança" },
  { valor: "CARTEIRA", label: "Carteira" },
  { valor: "INVESTIMENTO", label: "Investimento" },
];

export function tipoContaConhecido(tipo: string | undefined) {
  return tiposConta.find((t) => t.valor === tipo?.trim().toUpperCase());
}

export function labelTipoConta(tipo: string) {
  return tipoContaConhecido(tipo)?.label ?? tipo;
}
