import re
import os

base = "src/main/java/com/example/financas/controller/"

# 1. CategoriaController
f = os.path.join(base, "CategoriaController.java")
with open(f, 'r', encoding='utf-8') as file:
    content = file.read()

content = content.replace("import org.springframework.web.bind.annotation.*;", "import org.springframework.web.bind.annotation.*;\nimport org.springframework.security.core.annotation.AuthenticationPrincipal;\nimport com.example.financas.entity.Usuario;")
content = re.sub(r'public CategoriaResponse atualizar\(\s*@PathVariable Long id, @Valid @RequestBody CategoriaRequest request\)', r'public CategoriaResponse atualizar(@PathVariable Long id, @Valid @RequestBody CategoriaRequest request, @AuthenticationPrincipal Usuario usuario)', content)
content = re.sub(r'return categoriaService\.atualizar\(id, request\);', r'return categoriaService.atualizar(id, request, usuario.getIdUsuario());', content)
content = re.sub(r'public ResponseEntity<Void> remover\(@PathVariable Long id\)', r'public ResponseEntity<Void> remover(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario)', content)
content = re.sub(r'categoriaService\.remover\(id\);', r'categoriaService.remover(id, usuario.getIdUsuario());', content)

with open(f, 'w', encoding='utf-8') as file:
    file.write(content)

# 2. ContaController
f = os.path.join(base, "ContaController.java")
with open(f, 'r', encoding='utf-8') as file:
    content = file.read()

content = content.replace("import org.springframework.web.bind.annotation.*;", "import org.springframework.web.bind.annotation.*;\nimport org.springframework.security.core.annotation.AuthenticationPrincipal;\nimport com.example.financas.entity.Usuario;")
content = re.sub(r'public ContaResponse atualizar\(@PathVariable Long id, @Valid @RequestBody ContaRequest request\)', r'public ContaResponse atualizar(@PathVariable Long id, @Valid @RequestBody ContaRequest request, @AuthenticationPrincipal Usuario usuario)', content)
content = re.sub(r'return contaService\.atualizar\(id, request\);', r'return contaService.atualizar(id, request, usuario.getIdUsuario());', content)
content = re.sub(r'public ResponseEntity<Void> remover\(@PathVariable Long id\)', r'public ResponseEntity<Void> remover(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario)', content)
content = re.sub(r'contaService\.remover\(id\);', r'contaService.remover(id, usuario.getIdUsuario());', content)

with open(f, 'w', encoding='utf-8') as file:
    file.write(content)

# 3. TituloController
f = os.path.join(base, "TituloController.java")
with open(f, 'r', encoding='utf-8') as file:
    content = file.read()

content = content.replace("import org.springframework.web.bind.annotation.*;", "import org.springframework.web.bind.annotation.*;\nimport org.springframework.security.core.annotation.AuthenticationPrincipal;\nimport com.example.financas.entity.Usuario;")
content = re.sub(r'public TituloResponse atualizar\(\s*@PathVariable Long id, @Valid @RequestBody TituloRequest request\)', r'public TituloResponse atualizar(\n      @PathVariable Long id, @Valid @RequestBody TituloRequest request, @AuthenticationPrincipal Usuario usuario)', content)
content = re.sub(r'return tituloService\.atualizar\(id, request\);', r'return tituloService.atualizar(id, request, usuario.getIdUsuario());', content)

content = re.sub(r'public TituloResponse cancelar\(@PathVariable Long id\)', r'public TituloResponse cancelar(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario)', content)
content = re.sub(r'return tituloService\.cancelar\(id\);', r'return tituloService.cancelar(id, usuario.getIdUsuario());', content)

content = re.sub(r'public ResponseEntity<Void> remover\(@PathVariable Long id\)', r'public ResponseEntity<Void> remover(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario)', content)
content = re.sub(r'tituloService\.remover\(id\);', r'tituloService.remover(id, usuario.getIdUsuario());', content)

with open(f, 'w', encoding='utf-8') as file:
    file.write(content)

# 4. MovimentacaoController
f = os.path.join(base, "MovimentacaoController.java")
with open(f, 'r', encoding='utf-8') as file:
    content = file.read()

content = content.replace("import org.springframework.web.bind.annotation.*;", "import org.springframework.web.bind.annotation.*;\nimport org.springframework.security.core.annotation.AuthenticationPrincipal;\nimport com.example.financas.entity.Usuario;")
content = re.sub(r'public ResponseEntity<Void> estornar\(@PathVariable Long id\)', r'public ResponseEntity<Void> estornar(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario)', content)
content = re.sub(r'movimentacaoService\.estornar\(id\);', r'movimentacaoService.estornar(id, usuario.getIdUsuario());', content)

with open(f, 'w', encoding='utf-8') as file:
    file.write(content)

