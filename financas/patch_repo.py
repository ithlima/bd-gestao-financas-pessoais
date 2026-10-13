import re
import os

f = "src/main/java/com/example/financas/repository/MovimentacaoRepository.java"
with open(f, 'r', encoding='utf-8') as file:
    content = file.read()

content = content.replace("AND m.data BETWEEN :inicio AND :fim", "AND m.data BETWEEN :inicio AND :fim\n             AND m.is_transferencia = false")

with open(f, 'w', encoding='utf-8') as file:
    file.write(content)
