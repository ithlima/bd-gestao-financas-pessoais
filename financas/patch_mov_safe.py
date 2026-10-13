import os

f = "src/main/java/com/example/financas/entity/Movimentacao.java"
with open(f, 'r', encoding='utf-8') as file:
    content = file.read()

lines = content.split('\n')
new_lines = []
skip = False
for line in lines:
    if "@NotNull(message = \"A categoria" in line:
        skip = True
        new_lines.append("  @Column(name = \"is_transferencia\", nullable = false)")
        new_lines.append("  private Boolean isTransferencia = false;\n")
        new_lines.append("  @ManyToOne(fetch = FetchType.LAZY)")
        new_lines.append("  @JoinColumn(")
        new_lines.append("      name = \"categoria_id\",")
        new_lines.append("      foreignKey = @ForeignKey(name = \"fk_movimentacao_categoria\"))")
        new_lines.append("  private Categoria categoria;")
    elif skip and "private Categoria categoria;" in line:
        skip = False
    elif not skip:
        new_lines.append(line)

with open(f, 'w', encoding='utf-8') as file:
    file.write('\n'.join(new_lines))
