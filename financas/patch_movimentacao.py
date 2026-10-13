import re
import os

f = "src/main/java/com/example/financas/entity/Movimentacao.java"
with open(f, 'r', encoding='utf-8') as file:
    content = file.read()

# find the block for categoria
content = re.sub(
    r'@NotNull.*?A categoria[^"]*".*?@ManyToOne.*?optional = false\)\s*@JoinColumn\(\s*name = "categoria_id",\s*nullable = false,\s*foreignKey = @ForeignKey\(name = "fk_movimentacao_categoria"\)\)\s*private Categoria categoria;',
    r'@Column(name = "is_transferencia", nullable = false)\n  private Boolean isTransferencia = false;\n\n  @ManyToOne(fetch = FetchType.LAZY)\n  @JoinColumn(name = "categoria_id", foreignKey = @ForeignKey(name = "fk_movimentacao_categoria"))\n  private Categoria categoria;',
    content,
    flags=re.DOTALL
)

with open(f, 'w', encoding='utf-8') as file:
    file.write(content)
