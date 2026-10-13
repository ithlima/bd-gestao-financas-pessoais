import java.nio.file.*;
import java.io.IOException;

public class PatchIdor {
    public static void main(String[] args) throws IOException {
        String base = "src/main/java/com/example/financas/service/";
        
        // 1. MovimentacaoService
        Path mov = Paths.get(base + "MovimentacaoService.java");
        String movContent = new String(Files.readAllBytes(mov));
        movContent = movContent.replace("public void estornar(Long id) {", "public void estornar(Long id, Long usuarioId) {\n    Movimentacao movimentacao = buscarEntidade(id);\n    if (!movimentacao.getUsuario().getIdUsuario().equals(usuarioId)) throw new RegraNegocioException(\"Acesso negado\");");
        // Remove old lines that did Movimentacao movimentacao = buscarEntidade(id);
        movContent = movContent.replace("Movimentacao movimentacao = buscarEntidade(id);\n    Titulo titulo = movimentacao.getTitulo();", "Titulo titulo = movimentacao.getTitulo();");
        Files.write(mov, movContent.getBytes());

        // 2. TituloService
        Path tit = Paths.get(base + "TituloService.java");
        String titContent = new String(Files.readAllBytes(tit));
        titContent = titContent.replace("public TituloResponse cancelar(Long id) {", "public TituloResponse cancelar(Long id, Long usuarioId) {");
        titContent = titContent.replace("Titulo titulo = buscarEntidade(id);\n\n    if (titulo.getSituacao() == SituacaoTitulo.CANCELADO) {", "Titulo titulo = buscarEntidade(id);\n    if (!titulo.getUsuario().getIdUsuario().equals(usuarioId)) throw new RegraNegocioException(\"Acesso negado\");\n    if (titulo.getSituacao() == SituacaoTitulo.CANCELADO) {");
        
        titContent = titContent.replace("public void remover(Long id) {", "public void remover(Long id, Long usuarioId) {");
        titContent = titContent.replace("Titulo titulo = buscarEntidade(id);\n\n    if (movimentacaoService.temMovimentacaoVinculada(id)) {", "Titulo titulo = buscarEntidade(id);\n    if (!titulo.getUsuario().getIdUsuario().equals(usuarioId)) throw new RegraNegocioException(\"Acesso negado\");\n    if (movimentacaoService.temMovimentacaoVinculada(id)) {");

        titContent = titContent.replace("public TituloResponse atualizar(Long id, TituloRequest request) {", "public TituloResponse atualizar(Long id, TituloRequest request, Long usuarioId) {");
        titContent = titContent.replace("Titulo titulo = buscarEntidade(id);\n\n    if (titulo.getSituacao() == SituacaoTitulo.CANCELADO) {", "Titulo titulo = buscarEntidade(id);\n    if (!titulo.getUsuario().getIdUsuario().equals(usuarioId)) throw new RegraNegocioException(\"Acesso negado\");\n    if (titulo.getSituacao() == SituacaoTitulo.CANCELADO) {");

        Files.write(tit, titContent.getBytes());

        // 3. ContaService
        Path con = Paths.get(base + "ContaService.java");
        String conContent = new String(Files.readAllBytes(con));
        conContent = conContent.replace("public ContaResponse atualizar(Long id, ContaRequest request) {", "public ContaResponse atualizar(Long id, ContaRequest request, Long usuarioId) {");
        conContent = conContent.replace("Conta conta = buscarEntidade(id);\n\n    if (!conta.getNome().equals(request.nome())", "Conta conta = buscarEntidade(id);\n    if (!conta.getUsuario().getIdUsuario().equals(usuarioId)) throw new RegraNegocioException(\"Acesso negado\");\n    if (!conta.getNome().equals(request.nome())");

        conContent = conContent.replace("public void remover(Long id) {", "public void remover(Long id, Long usuarioId) {");
        conContent = conContent.replace("Conta conta = buscarEntidade(id);\n\n    if (movimentacaoRepository.existsByContaIdConta(id)) {", "Conta conta = buscarEntidade(id);\n    if (!conta.getUsuario().getIdUsuario().equals(usuarioId)) throw new RegraNegocioException(\"Acesso negado\");\n    if (movimentacaoRepository.existsByContaIdConta(id)) {");

        Files.write(con, conContent.getBytes());

        // 4. CategoriaService
        Path cat = Paths.get(base + "CategoriaService.java");
        String catContent = new String(Files.readAllBytes(cat));
        catContent = catContent.replace("public CategoriaResponse atualizar(Long id, CategoriaRequest request) {", "public CategoriaResponse atualizar(Long id, CategoriaRequest request, Long usuarioId) {");
        catContent = catContent.replace("Categoria categoria = buscarEntidade(id);\n\n    if (!categoria.getNome().equals(request.nome())", "Categoria categoria = buscarEntidade(id);\n    if (!categoria.getUsuario().getIdUsuario().equals(usuarioId)) throw new RegraNegocioException(\"Acesso negado\");\n    if (!categoria.getNome().equals(request.nome())");

        catContent = catContent.replace("public void remover(Long id) {", "public void remover(Long id, Long usuarioId) {");
        catContent = catContent.replace("Categoria categoria = buscarEntidade(id);\n\n    if (tituloRepository.existsByCategoriaIdCategoria(id)) {", "Categoria categoria = buscarEntidade(id);\n    if (!categoria.getUsuario().getIdUsuario().equals(usuarioId)) throw new RegraNegocioException(\"Acesso negado\");\n    if (tituloRepository.existsByCategoriaIdCategoria(id)) {");

        Files.write(cat, catContent.getBytes());
    }
}
