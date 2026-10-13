import java.nio.file.*;
import java.io.IOException;

public class PatchControllers {
    public static void main(String[] args) throws IOException {
        String base = "src/main/java/com/example/financas/controller/";
        
        // 1. MovimentacaoController
        Path mov = Paths.get(base + "MovimentacaoController.java");
        String movContent = new String(Files.readAllBytes(mov));
        movContent = movContent.replace("import org.springframework.web.bind.annotation.*;", "import org.springframework.web.bind.annotation.*;\nimport org.springframework.security.core.annotation.AuthenticationPrincipal;\nimport com.example.financas.entity.Usuario;");
        movContent = movContent.replace("public ResponseEntity<Void> estornar(@PathVariable Long id) {", "public ResponseEntity<Void> estornar(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {");
        movContent = movContent.replace("movimentacaoService.estornar(id);", "movimentacaoService.estornar(id, usuario.getIdUsuario());");
        Files.write(mov, movContent.getBytes());

        // 2. TituloController
        Path tit = Paths.get(base + "TituloController.java");
        String titContent = new String(Files.readAllBytes(tit));
        titContent = titContent.replace("import org.springframework.web.bind.annotation.*;", "import org.springframework.web.bind.annotation.*;\nimport org.springframework.security.core.annotation.AuthenticationPrincipal;\nimport com.example.financas.entity.Usuario;");
        
        titContent = titContent.replace("public TituloResponse cancelar(@PathVariable Long id) {", "public TituloResponse cancelar(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {");
        titContent = titContent.replace("return tituloService.cancelar(id);", "return tituloService.cancelar(id, usuario.getIdUsuario());");
        
        titContent = titContent.replace("public ResponseEntity<Void> remover(@PathVariable Long id) {", "public ResponseEntity<Void> remover(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {");
        titContent = titContent.replace("tituloService.remover(id);", "tituloService.remover(id, usuario.getIdUsuario());");

        titContent = titContent.replace("public TituloResponse atualizar(\n      @PathVariable Long id, @Valid @RequestBody TituloRequest request) {", "public TituloResponse atualizar(\n      @PathVariable Long id, @Valid @RequestBody TituloRequest request, @AuthenticationPrincipal Usuario usuario) {");
        titContent = titContent.replace("return tituloService.atualizar(id, request);", "return tituloService.atualizar(id, request, usuario.getIdUsuario());");
        
        Files.write(tit, titContent.getBytes());

        // 3. ContaController
        Path con = Paths.get(base + "ContaController.java");
        String conContent = new String(Files.readAllBytes(con));
        conContent = conContent.replace("import org.springframework.web.bind.annotation.*;", "import org.springframework.web.bind.annotation.*;\nimport org.springframework.security.core.annotation.AuthenticationPrincipal;\nimport com.example.financas.entity.Usuario;");
        
        conContent = conContent.replace("public ContaResponse atualizar(@PathVariable Long id, @Valid @RequestBody ContaRequest request) {", "public ContaResponse atualizar(@PathVariable Long id, @Valid @RequestBody ContaRequest request, @AuthenticationPrincipal Usuario usuario) {");
        conContent = conContent.replace("return contaService.atualizar(id, request);", "return contaService.atualizar(id, request, usuario.getIdUsuario());");
        
        conContent = conContent.replace("public ResponseEntity<Void> remover(@PathVariable Long id) {", "public ResponseEntity<Void> remover(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {");
        conContent = conContent.replace("contaService.remover(id);", "contaService.remover(id, usuario.getIdUsuario());");
        Files.write(con, conContent.getBytes());

        // 4. CategoriaController
        Path cat = Paths.get(base + "CategoriaController.java");
        String catContent = new String(Files.readAllBytes(cat));
        catContent = catContent.replace("import org.springframework.web.bind.annotation.*;", "import org.springframework.web.bind.annotation.*;\nimport org.springframework.security.core.annotation.AuthenticationPrincipal;\nimport com.example.financas.entity.Usuario;");
        
        catContent = catContent.replace("public CategoriaResponse atualizar(@PathVariable Long id, @Valid @RequestBody CategoriaRequest request) {", "public CategoriaResponse atualizar(@PathVariable Long id, @Valid @RequestBody CategoriaRequest request, @AuthenticationPrincipal Usuario usuario) {");
        catContent = catContent.replace("return categoriaService.atualizar(id, request);", "return categoriaService.atualizar(id, request, usuario.getIdUsuario());");
        
        catContent = catContent.replace("public ResponseEntity<Void> remover(@PathVariable Long id) {", "public ResponseEntity<Void> remover(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {");
        catContent = catContent.replace("categoriaService.remover(id);", "categoriaService.remover(id, usuario.getIdUsuario());");
        Files.write(cat, catContent.getBytes());
    }
}
