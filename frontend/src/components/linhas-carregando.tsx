import { TableCell, TableRow } from "@/components/ui/table";

export function LinhasCarregando({ celulas, linhas = 6 }: { celulas: (string | false)[]; linhas?: number }) {
  return Array.from({ length: linhas }).map((_, linha) => (
    <TableRow key={linha} aria-hidden>
      {celulas.map((classe, indice) =>
        classe === false ? null : (
          <TableCell key={indice} className={classe}>
            <span />
          </TableCell>
        ),
      )}
    </TableRow>
  ));
}
